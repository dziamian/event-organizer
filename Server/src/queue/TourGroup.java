package queue;

import server.Guide;

import java.util.ArrayList;

public class TourGroup {

    private final ArrayList<QueueTicket> tickets;
    private final static int maxTickets = 3;
    private final ArrayList<Room.Reservation> reservations;
    private final static int maxReservations = 1;

    private int penaltyLevel = 0;

    private Room currentRoom;

    private final ArrayList<server.Guide> guides;
    private final static int maxGuides = 2;

    public TourGroup() {
        this.tickets = new ArrayList<>();
        this.reservations = new ArrayList<>();
        this.currentRoom = null;
        this.guides = new ArrayList<>();
    }

    /**
     * Increments current level of penalty induced for abandoning reservation for this group
     */
    public void increasePenaltyLevel() {
        ++penaltyLevel;
    }

    /**
     * @return Current penalty level of this group
     */
    public int getCurrentPenaltyLevel() {
        return penaltyLevel;
    }

    /**
     * @return Array of Rooms for which this group has ticket
     */
    public Room[] getTicketRooms() {
        Room[] rooms = new Room[tickets.size()];
        for (int i = 0; i < rooms.length; ++i) {
            rooms[i] = tickets.get(i).getDestination();
        }
        return rooms;
    }

    public QueueTicket getTicketForRoom(Room room) {
        for (var ticket : tickets) {
            if (ticket.getDestination() == room) {
                return ticket;
            }
        }
        return null;
    }

    public boolean removeTicket(QueueTicket ticket) {
        return tickets.remove(ticket);
    }

    /**
     * Adds guide to this group's guide list
     * @param guide Guide to add
     * @return true if guide has been added, false otherwise
     */
    public boolean addGuide(Guide guide) {
        if (guides.size() < maxGuides && guide != null) {
            try {
                guides.add(guide);
                return true;
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return false;
    }

    public boolean removeGuide(Guide guide) {
        try {
            return guides.remove(guide);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    protected boolean canAddReservation() {
        return reservations.size() < maxReservations;
    }

    private boolean canAddTicket() {
        return tickets.size() < maxTickets;
    }

    public boolean hasTicketFor(Room room) {
        for (TourGroup.QueueTicket ticket : tickets) {
            if (ticket.destination == room)
                return true;
        }
        return false;
    }

    protected void addReservation(Room.Reservation reservation) {
        if (reservation != null)
            reservations.add(reservation);
    }

    protected void removeReservation(Room.Reservation reservation) {
        if (reservation != null)
            reservations.remove(reservation);
    }

    protected QueueTicket createTicket(Room destination) {
        if (destination != null) {
            QueueTicket ticket = new QueueTicket(this,destination);
            if (canAddTicket() && !hasTicketFor(destination) && tickets.add(ticket)) {
                return ticket;
            }
        }
        return null;
    }

    protected void setCurrentRoom(Room currentRoom) {
        if (currentRoom != null)
            this.currentRoom = currentRoom;
    }

    protected void setCurrentRoomNull() {
        this.currentRoom = null;
    }

    //INNER CLASSES-------------------------------------------------------------------------------------------------

    public class QueueTicket {
        private final TourGroup owner;
        private final Room destination;
        private int timesAsked;

        private int groupingResponse; // (-2) - BRAK UDZIALU, (-1) - NIE, (0) - OCZEKIWANIE, (1) - TAK

        public QueueTicket(TourGroup owner, Room destination) {
            this.owner = owner;
            this.destination = destination;
            this.timesAsked = 0;
        }

        public TourGroup getOwner() {
            return owner;
        }

        protected int getGroupingResponse() {
            return groupingResponse;
        }

        public int getTimesAsked() {
            return timesAsked;
        }

        /**
         * Temporary, for removal
         * @return Name of this ticket's destination room
         */
        public String getRoomIdentifier() {
            return destination.getInfoFixed().getName();
        }

        public Room getDestination() {
            return this.destination;
        }

        protected void increaseTimesAsked() {
            ++timesAsked;
        }

        protected void sendNotificationAboutGrouping() {
            /*for (var guide : owner.guides) {
                guide.out.writeObject(...);
            }*/
            ///send notification about grouping to room
            groupingResponse = 0; //poki co brak odpowiedzi
        }

        protected void setNoParticipation() {
            groupingResponse = -2;
        }

//        public void respondAboutGrouping(int response) {
//            groupingResponse = response;
//            destination.queue.updateFullyGroupedStatus();
//        }
    }
}
