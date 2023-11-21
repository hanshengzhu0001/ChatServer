package org.cis1200;

import java.util.*;

/*
 * Make sure to write your own tests in ServerModelTest.java.
 * The tests we provide for each task are NOT comprehensive!
 */

/**
 * The {@code ServerModel} is the class responsible for tracking the
 * state of the server, including its current users and the channels
 * they are in.
 * This class is used by subclasses of {@link Command} to:
 * 1. handle commands from clients, and
 * 2. handle commands from {@link ServerBackend} to coordinate
 * Client connection/disconnection.
 */

public final class ServerModel {

    /**
     * Constructs a {@code ServerModel}. Make sure to initialize any collections
     * used to model the server state here.
     */

    public static class Client implements Comparable<Client> {
        private final int userId;

        private String nickname;

        public Client(int userId, String nickname) {
            this.userId = userId;
            this.nickname = nickname;
        }

        public int compareTo(Client other) {
            return Integer.compare(this.userId, other.userId);
        }
    }

    public static class Channel implements Comparable<Channel> {
        private final String nm;

        private final Client owner;

        private boolean inviteOnly;

        private TreeSet<Integer> userIds; // IDs of users in this Channel

        public Channel(String nm, Client owner) {
            this.nm = nm;
            this.owner = owner;
            this.userIds = new TreeSet<>();
        }

        public int compareTo(Channel other) {
            return this.nm.compareTo(other.nm);
        } // since nm of each Channel is unique,
          // can identify equality of Channel through names
    }

    private TreeMap<Integer, Client> idByClient; // allows lookup for users using IDs
    private TreeMap<String, Channel> namebyChannel; // allows lookup for channels using names

    private TreeMap<String, Client> nicknamebyclient; // allows lookup for user iD using nicknames

    private TreeSet<Integer> activeuserIDs; // users connected to the server

    public ServerModel() { // constructor initializes every collection
        this.idByClient = new TreeMap<>();
        this.namebyChannel = new TreeMap<>();
        this.nicknamebyclient = new TreeMap<>();
        this.activeuserIDs = new TreeSet<>();
    }

    // =========================================================================
    // == Task 2: Basic Server model queries
    // == These functions provide helpful ways to test the state of your model.
    // == You may also use them in later tasks.
    // =========================================================================

    /**
     * Gets the user iD currently associated with the given
     * nickname. The returned iD is -1 if the nickname is not
     * currently in use.
     *
     * @param nickname The nickname for which to get the associated user iD
     * @return The user iD of the user with the argued nickname if
     *         such a user exists, otherwise -1
     */
    public int getUserId(String nickname) {
        if (nicknamebyclient.containsKey(nickname)) {
            Client current = nicknamebyclient.get(nickname);
            return current.userId;
        }
        return -1;
    }

    /**
     * Gets the nickname currently associated with the given user
     * iD. The returned nickname is null if the user iD is not
     * currently in use.
     *
     * @param userId The user iD for which to get the associated
     *               nickname
     * @return The nickname of the user with the argued user iD if
     *         such a user exists, otherwise null
     */
    public String getNickname(int userId) {
        if (idByClient.containsKey(userId)) {
            Client current = idByClient.get(userId); // get Client using iD
            return current.nickname;
        }
        return null;
    }

    /**
     * Gets a collection of the nicknames of all users who are
     * registered with the server. Changes to the returned collection
     * should not affect the server state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of registered user nicknames
     */
    public Collection<String> getRegisteredUsers() {
        Collection<String> nn = new TreeSet<String>(); // initialize nicknames as TreeSet
        // iterate through the set of user IDs
        for (Integer iD : activeuserIDs) {
            Client current = idByClient.get(iD);
            nn.add(current.nickname);
        }
        return nn;
    }

    /**
     * Gets a collection of the names of all the channels that are
     * present on the server. Changes to the returned collection
     * should not affect the server state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of Channel names
     */
    public Collection<String> getChannels() {
        Collection<String> channelNames = new TreeSet<String>();
        for (Map.Entry<String, Channel> entry : namebyChannel.entrySet()) {
            String channelName = entry.getKey();
            channelNames.add(channelName);
        }
        // iterate over all entries of the map
        return channelNames;
    }

    /**
     * Gets a collection of the nicknames of all the users in a given
     * Channel. The collection is empty if no Channel with the given
     * nm exists. Modifications to the returned collection should
     * not affect the server state.
     *
     * This method is provided for testing.
     *
     * @param channelName The Channel for which to get member nicknames
     * @return A collection of all user nicknames in the Channel
     */
    public Collection<String> getUsersInChannel(String channelName) {
        if (namebyChannel.containsKey(channelName)) {
            Collection<String> userNicknames = new TreeSet<String>(); //create a copy
            Channel ch = namebyChannel.get(channelName);
            for (Integer iD : ch.userIds) {
                Client current = idByClient.get(iD);
                userNicknames.add(current.nickname);
            }
            // iterate over all entries of the map
            return userNicknames;
        }
        return new TreeSet<String>();
    }

    /**
     * Gets the nickname of the owner of the given Channel. The result
     * is {@code null} if no Channel with the given nm exists.
     *
     * This method is provided for testing.
     *
     * @param channelName The Channel for which to get the owner nickname
     * @return The nickname of the Channel owner if such a Channel
     *         exists; otherwise, return null
     */
    public String getOwner(String channelName) {
        if (namebyChannel.containsKey(channelName)) {
            Channel ch = namebyChannel.get(channelName);
            Client ow = ch.owner;
            return ow.nickname;
        }
        return null;
    }

    // ===============================================
    // == Task 3: Connections and Setting Nicknames ==
    // ===============================================

    /**
     * This method is automatically called by the backend when a new Client
     * connects to the server. It should generate a default nickname with
     * {@link #generateUniqueNickname()}, store the new user's iD and username
     * in your data structures for {@link ServerModel} state, and construct
     * and return a {@link Broadcast} object using
     * {@link Broadcast#connected(String)}}.
     *
     * @param userId The new user's unique iD (automatically created by the
     *               backend)
     * @return The {@link Broadcast} object generated by calling
     *         {@link Broadcast#connected(String)} with the proper parameter
     */
    public Broadcast registerUser(int userId) {
        if (activeuserIDs.contains(userId)) {
            return null;
        }
        String nickname = generateUniqueNickname();
        activeuserIDs.add(userId);
        Client current = new Client(userId, nickname);
        idByClient.put(userId, current);
        nicknamebyclient.put(nickname, current);
        // We have taken care of generating the nickname and returning
        // the Broadcast for you. You need to modify this method to
        // store the new user's iD and username in this model's internal state.
        return Broadcast.connected(nickname);
    }

    /**
     * Helper for {@link #registerUser(int)}. (Nothing to do here.)
     *
     * Generates a unique nickname of the form "UserX", where X is the
     * smallest non-negative integer that yields a unique nickname for a user.
     * 
     * @return The generated nickname
     */
    private String generateUniqueNickname() {
        int suffix = 0;
        String nickname;
        Collection<String> existingUsers = getRegisteredUsers();
        do {
            nickname = "User" + suffix++;
        } while (existingUsers.contains(nickname));
        return nickname;
    }

    /**
     * This method is automatically called by the backend when a Client
     * disconnects from the server. This method should take the following
     * actions, not necessarily in this order:
     *
     * (1) All users who shared a Channel with the disconnected user should be
     * notified that they left
     * (2) All channels owned by the disconnected user should be deleted
     * (3) The disconnected user's information should be removed from
     * {@link ServerModel}'s internal state
     * (4) Construct and return a {@link Broadcast} object using
     * {@link Broadcast#disconnected(String, Collection)}.
     *
     * @param userId The unique iD of the user to deregister
     * @return The {@link Broadcast} object generated by calling
     *         {@link Broadcast#disconnected(String, Collection)} with the proper
     *         parameters
     */

    public TreeSet<String> getallchannels(int userId) {
        TreeSet<String> ownedChannels = new TreeSet<String>();
        String nn = getNickname(userId);
        for (Map.Entry<String, Channel> entry : namebyChannel.entrySet()) {
            String channelName = entry.getKey();
            Channel ch = entry.getValue();
            if (getOwner(channelName).equals(nn)) {
                ownedChannels.add(channelName);
            }
        }
        return ownedChannels;
    }

    public Broadcast deregisterUser(int userId) {
        if (activeuserIDs.contains(userId)) {
            String nn = getNickname(userId);
            TreeSet<String> usersToNotify = new TreeSet<>();

            // collect all channels owned by the Client
            TreeSet<String> ownedChannels = getallchannels(userId);

            // broadcast to users who share Channel that the user left
            for (Map.Entry<String, Channel> entry : namebyChannel.entrySet()) {
                String nm = entry.getKey();
                Channel ch = entry.getValue();
                if (ch.userIds.contains(userId)) {
                    ch.userIds.remove(userId); //remove this user from the Channel
                    Broadcast.disconnected(nn, getUsersInChannel(nm));
                    usersToNotify.addAll(getUsersInChannel(nm));
                }
            }

            // delete all channels owned by the Client
            for (String channelName : ownedChannels) {
                if (getOwner(channelName).equals(nn)) {
                    namebyChannel.remove(channelName); // removing keys removes all entries
                }
            }

            // remove user's information from SeverModel's internal state
            nicknamebyclient.remove(nn);
            idByClient.remove(userId);
            activeuserIDs.remove(userId);

            return Broadcast.disconnected(nn, usersToNotify);
            //return braodcast object including all users notified
        }
        return null;
    }

    /**
     * This method is called when a user wants to change their nickname.
     * 
     * @param nickCommand The {@link NicknameCommand} object containing
     *                    all information needed to attempt a nickname change
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the nickname
     *         change is successful. The command should be the original nickCommand
     *         and the collection of recipients should be any clients who
     *         share at least one Channel with the sender, including the sender.
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#INVALID_NAME} if the proposed nickname
     *         is not valid according to
     *         {@link ServerModel#isValidName(String)}
     *         (2) {@link ServerResponse#NAME_ALREADY_IN_USE} if there is
     *         already a user with the proposed nickname
     */
    public Broadcast changeNickname(NicknameCommand nickCommand) {
        Integer iD = nickCommand.getSenderId();
        String nn = nickCommand.getNewNickname();

        if (activeuserIDs.contains(iD)) {
            if (isValidName(nn)) {
                TreeSet<String> channelNames = new TreeSet<>();
                TreeSet<String> usersToNotify = new TreeSet<>();

                // Check if the new nickname is already in use in the server
                if (nicknamebyclient.containsKey(nn)) {
                    return Broadcast.error(nickCommand, ServerResponse.NAME_ALREADY_IN_USE);
                }

                for (Map.Entry<String, Channel> entry : namebyChannel.entrySet()) {
                    Channel current = entry.getValue();
                    if (current.userIds.contains(iD)) {
                        String channelName = entry.getKey();
                        channelNames.add(channelName); //Add all channels containing the user
                    }
                }

                for (String channelName : channelNames) {
                    Collection<String> nicknames = getUsersInChannel(channelName);
                    usersToNotify.addAll(nicknames); // Add all recipients
                }

                //Change the nickname
                Client current = idByClient.get(iD);
                nicknamebyclient.remove(current.nickname); //remove original
                current.nickname = nn;
                nicknamebyclient.put(nn,current); //add new

                return Broadcast.okay(nickCommand, usersToNotify);
            }
            return Broadcast.error(nickCommand, ServerResponse.INVALID_NAME);
        }
        return null;
    }

    /**
     * Determines if a given nickname is valid or invalid (contains at least
     * one alphanumeric character, and no non-alphanumeric characters).
     * (Nothing to do here.)
     * 
     * @param nm The Channel or nickname string to validate
     * @return true if the string is a valid nm
     */
    public static boolean isValidName(String nm) {
        if (nm == null || nm.isEmpty()) {
            return false;
        }
        for (char c : nm.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    // ===================================
    // == Task 4: Channels and Messages ==
    // ===================================

    /**
     * This method is called when a user wants to create a Channel.
     * You can ignore the privacy aspect of this method for task 4, but
     * make sure you come back and implement it in task 5.
     * 
     * @param createCommand The {@link CreateCommand} object containing all
     *                      information needed to attempt Channel creation
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the Channel
     *         creation is successful. The only recipient should be the new
     *         Channel's owner.
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#INVALID_NAME} if the proposed
     *         Channel nm is not valid according to
     *         {@link ServerModel#isValidName(String)}
     *         (2) {@link ServerResponse#CHANNEL_ALREADY_EXISTS} if there is
     *         already a Channel with the proposed nm
     */
    public Broadcast createChannel(CreateCommand createCommand) {
        Integer iD = createCommand.getSenderId();
        Client owner = idByClient.get(iD);
        String channelName = createCommand.getChannel();
        Collection<String> usersToNotify = new TreeSet<>();

        if (isValidName(channelName)) {
            if (namebyChannel.containsKey(channelName)) {
                return Broadcast.error(createCommand,ServerResponse.CHANNEL_ALREADY_EXISTS);
            }
            Channel ch = new Channel(channelName, owner);

            ch.inviteOnly = createCommand.isInviteOnly();
            ch.userIds.add(iD); //add the owner to the Channel's user TreeSet
            namebyChannel.put(channelName, ch); //add the new Channel to the treemap
            usersToNotify.add(owner.nickname); //add the recipient
            return Broadcast.okay(createCommand,usersToNotify);
        }
        return Broadcast.error(createCommand,ServerResponse.INVALID_NAME);
    }

    /**
     * This method is called when a user wants to join a Channel.
     * You can ignore the privacy aspect of this method for task 4, but
     * make sure you come back and implement it in task 5.
     * 
     * @param joinCommand The {@link JoinCommand} object containing all
     *                    information needed for the user's join attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#names(Command, Collection, String)} if the user
     *         joins the Channel successfully. The recipients should be all
     *         people in the joined Channel (including the sender).
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no
     *         Channel with the specified nm
     *         (2) (after Task 5) {@link ServerResponse#JOIN_PRIVATE_CHANNEL} if
     *         the sender is attempting to join a private Channel
     */
    public Broadcast joinChannel(JoinCommand joinCommand) {
        Integer iD = joinCommand.getSenderId();
        Client user = idByClient.get(iD);
        String channelName = joinCommand.getChannel();
        Collection<String> usersToNotify = new TreeSet<>();

        if (namebyChannel.containsKey(channelName)) {
            Channel ch = namebyChannel.get(channelName);
            if (ch.inviteOnly) {
                return Broadcast.error(joinCommand,ServerResponse.JOIN_PRIVATE_CHANNEL);
            }
            if (!ch.userIds.contains(iD)){
                ch.userIds.add(iD);
                usersToNotify.addAll(getUsersInChannel(channelName));
                return Broadcast.names(joinCommand,usersToNotify,getOwner(channelName));
            }
        }
        return Broadcast.error(joinCommand,ServerResponse.NO_SUCH_CHANNEL);
    }

    /**
     * This method is called when a user wants to send a message to a Channel.
     * 
     * @param messageCommand The {@link MessageCommand} object containing all
     *                       information needed for the messaging attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the message
     *         attempt is successful. The recipients should be all clients
     *         in the Channel.
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no
     *         Channel with the specified nm
     *         (2) {@link ServerResponse#USER_NOT_IN_CHANNEL} if the sender is
     *         not in the Channel they are trying to send the message to
     */
    public Broadcast sendMessage(MessageCommand messageCommand) {
        Integer iD = messageCommand.getSenderId();
        String channelName = messageCommand.getChannel();
        Collection<String> usersToNotify = new TreeSet<>();

        if (!namebyChannel.containsKey(channelName)) {
            return Broadcast.error(messageCommand,ServerResponse.NO_SUCH_CHANNEL);
        }

        Channel ch = namebyChannel.get(channelName);

        if (!ch.userIds.contains(iD)) {
            return Broadcast.error(messageCommand,ServerResponse.USER_NOT_IN_CHANNEL);
        }

        usersToNotify.addAll(getUsersInChannel(channelName));
        return Broadcast.okay(messageCommand,usersToNotify);
    }

    /**
     * This method is called when a user wants to leave a Channel.
     * 
     * @param leaveCommand The {@link LeaveCommand} object containing all
     *                     information about the user's leave attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the user leaves
     *         the Channel successfully. The recipients should be all clients
     *         who were in the Channel, including the user who left.
     * 
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no
     *         Channel with the specified nm
     *         (2) {@link ServerResponse#USER_NOT_IN_CHANNEL} if the sender is
     *         not in the Channel they are trying to leave
     */
    public Broadcast leaveChannel(LeaveCommand leaveCommand) {
        Integer iD = leaveCommand.getSenderId();
        String channelName = leaveCommand.getChannel();
        Collection<String> usersToNotify = new TreeSet<>();

        if (!namebyChannel.containsKey(channelName)) {
            return Broadcast.error(leaveCommand,ServerResponse.NO_SUCH_CHANNEL);
        }

        Channel ch = namebyChannel.get(channelName);

        if (!ch.userIds.contains(iD)) {
            return Broadcast.error(leaveCommand,ServerResponse.USER_NOT_IN_CHANNEL);
        }

        usersToNotify.addAll(getUsersInChannel(channelName));
        ch.userIds.remove(iD); //remove user from Channel
        return Broadcast.okay(leaveCommand,usersToNotify);
    }

    // =============================
    // == Task 5: Channel Privacy ==
    // =============================

    // Go back to createChannel and joinChannel and add
    // all privacy-related functionalities, then delete this when you're done.

    /**
     * This method is called when a Channel's owner adds a user to that Channel.
     * 
     * @param inviteCommand The {@link InviteCommand} object containing all
     *                      information needed for the invite attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#names(Command, Collection, String)} if the user
     *         joins the Channel successfully as a result of the invite.
     *         The recipients should be all people in the joined Channel
     *         (including the new user).
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_USER} if the invited user
     *         does not exist
     *         (2) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no Channel
     *         with the specified nm
     *         (3) {@link ServerResponse#INVITE_TO_PUBLIC_CHANNEL} if the
     *         invite refers to a public Channel
     *         (4) {@link ServerResponse#USER_NOT_OWNER} if the sender is not
     *         the owner of the Channel
     */
    public Broadcast inviteUser(InviteCommand inviteCommand) {
        Integer senderID = inviteCommand.getSenderId();
        String userToInvite = inviteCommand.getUserToInvite();
        String channelName = inviteCommand.getChannel();
        TreeSet<String> usersToNotify = new TreeSet<>();

        //Should I also check if the sender iD exists?

        if (!nicknamebyclient.containsKey(userToInvite)) {
            return Broadcast.error(inviteCommand,ServerResponse.NO_SUCH_USER);
        }

        if (!namebyChannel.containsKey(channelName)) {
            return Broadcast.error(inviteCommand,ServerResponse.NO_SUCH_CHANNEL);
        }

        Client sender = idByClient.get(senderID);
        Client invited = nicknamebyclient.get(userToInvite);
        Integer invitedID = invited.userId;
        Channel ch = namebyChannel.get(channelName);
        String owner = getOwner(channelName);

        if (!ch.inviteOnly) {
            return Broadcast.error(inviteCommand,ServerResponse.INVITE_TO_PUBLIC_CHANNEL);
        }

        if (!sender.nickname.equals(owner)) {
            return Broadcast.error(inviteCommand,ServerResponse.USER_NOT_OWNER);
        }

        ch.userIds.add(invitedID);
        usersToNotify.addAll(getUsersInChannel(channelName));
        return Broadcast.names(inviteCommand,usersToNotify,owner);
    }

    /**
     * This method is called when a Channel's owner removes a user from
     * that Channel.
     * 
     * @param kickCommand The {@link KickCommand} object containing all
     *                    information needed for the kick attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the user is
     *         successfully kicked from the Channel. The recipients should be
     *         all clients who were in the Channel, including the user
     *         who was kicked.
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_USER} if the user being kicked
     *         does not exist
     *         (2) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no Channel
     *         with the specified nm
     *         (3) {@link ServerResponse#USER_NOT_IN_CHANNEL} if the
     *         user being kicked is not a member of the Channel
     *         (4) {@link ServerResponse#USER_NOT_OWNER} if the sender is not
     *         the owner of the Channel
     */
    public Broadcast kickUser(KickCommand kickCommand) {
        Integer senderID = kickCommand.getSenderId();
        String userToKick = kickCommand.getUserToKick();
        String channelName = kickCommand.getChannel();
        TreeSet<String> usersToNotify = new TreeSet<>();

        if (!nicknamebyclient.containsKey(userToKick)) {
            return Broadcast.error(kickCommand,ServerResponse.NO_SUCH_USER);
        }

        if (!namebyChannel.containsKey(channelName)) {
            return Broadcast.error(kickCommand,ServerResponse.NO_SUCH_CHANNEL);
        }

        Client sender = idByClient.get(senderID);
        Client kicked = nicknamebyclient.get(userToKick);
        Integer kickedID = kicked.userId;
        Channel ch = namebyChannel.get(channelName);
        String owner = getOwner(channelName);

        if (!ch.userIds.contains(kickedID)) {
            return Broadcast.error(kickCommand,ServerResponse.USER_NOT_IN_CHANNEL);
        }

        if (!sender.nickname.equals(owner)) {
            return Broadcast.error(kickCommand,ServerResponse.USER_NOT_OWNER);
        }

        usersToNotify.addAll(getUsersInChannel(channelName));

        ch.userIds.remove(kickedID);
        return Broadcast.okay(kickCommand,usersToNotify);
    }

}
