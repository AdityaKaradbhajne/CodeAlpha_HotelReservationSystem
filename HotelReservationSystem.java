import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HotelReservationSystem {

    static final String DATA_FILE = "bookings.dat";
 
    // Room Category ------------------------------------------------------------------------
    enum RoomType {
        STANDARD("Standard",  1500),
        DELUXE  ("Deluxe",    3000),
        SUITE   ("Suite",     6000);
 
        final String label;
        final double pricePerNight;
 
        RoomType(String label, double pricePerNight) {
            this.label = label;
            this.pricePerNight = pricePerNight;
        }
    }
 
    // Room -----------------------------------------------------------------------------------------
    static class Room implements Serializable {
        int roomNumber;
        RoomType type;
        boolean isAvailable;
 
        Room(int roomNumber, RoomType type) {
            this.roomNumber = roomNumber;
            this.type = type;
            this.isAvailable = true;
        }
 
        @Override
        public String toString() {
            return String.format("Room %3d | %-8s | $%.0f/night | %s",
                    roomNumber, type.label, type.pricePerNight,
                    isAvailable ? "✅ Available" : "❌ Booked");
        }
    }
 
    // Reservation -----------------------------------------------------------------------------------
    static class Reservation implements Serializable {
        static int counter = 1000;
        int bookingId;
        String guestName, phone;
        int roomNumber;
        RoomType roomType;
        LocalDate checkIn, checkOut;
        double totalAmount;
        boolean paid;
 
        Reservation(String guestName, String phone, int roomNumber,
                    RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
            this.bookingId   = ++counter;
            this.guestName   = guestName;
            this.phone       = phone;
            this.roomNumber  = roomNumber;
            this.roomType    = roomType;
            this.checkIn     = checkIn;
            this.checkOut    = checkOut;
            long nights      = ChronoUnit.DAYS.between(checkIn, checkOut);
            this.totalAmount = nights * roomType.pricePerNight;
            this.paid        = false;
        }
 
        long getNights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }
 
        void printReceipt() {
            System.out.println("\n┌──────────────── BOOKING RECEIPT ────────────────┐");
            System.out.printf( "│  Booking ID   : %-31d │%n", bookingId);
            System.out.printf( "│  Guest Name   : %-31s │%n", guestName);
            System.out.printf( "│  Phone        : %-31s │%n", phone);
            System.out.printf( "│  Room No.     : %-31d │%n", roomNumber);
            System.out.printf( "│  Room Type    : %-31s │%n", roomType.label);
            System.out.printf( "│  Check-In     : %-31s │%n", checkIn);
            System.out.printf( "│  Check-Out    : %-31s │%n", checkOut);
            System.out.printf( "│  Nights       : %-31d │%n", getNights());
            System.out.printf( "│  Rate/Night   : $%-30.2f │%n", roomType.pricePerNight);
            System.out.printf( "│  Total Amount : $%-30.2f │%n", totalAmount);
            System.out.printf( "│  Payment      : %-31s │%n", paid ? "✔ PAID" : "⚠ PENDING");
            System.out.println("└──────────────────────────────────────────────────┘");
        }
    }
 
    // Hotel System ----------------------------------------------------------------------------------------
    static List<Room> rooms = new ArrayList<>();
    static List<Reservation> reservations = new ArrayList<>();
 
    static {
        // Rooms 101–105: Standard, 201–205: Deluxe, 301–304: Suite
        for (int i = 101; i <= 105; i++) rooms.add(new Room(i, RoomType.STANDARD));
        for (int i = 201; i <= 205; i++) rooms.add(new Room(i, RoomType.DELUXE));
        for (int i = 301; i <= 304; i++) rooms.add(new Room(i, RoomType.SUITE));
    }
 
    // Main ----------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        loadData();
        Scanner sc = new Scanner(System.in);
 
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Hotel Reservation System — v1.0    ║");
        System.out.println("║    Welcome to AlphaStay Hotel 🏨     ║");
        System.out.println("╚══════════════════════════════════════╝");
 
        boolean running = true;
        while (running) {
            System.out.println("\n┌─ MENU ──────────────────────────────────┐");
            System.out.println("│  1. View Available Rooms                │");
            System.out.println("│  2. Make a Reservation                  │");
            System.out.println("│  3. Cancel a Reservation                │");
            System.out.println("│  4. View All Bookings                   │");
            System.out.println("│  5. Pay for Booking                     │");
            System.out.println("│  6. Search Booking by ID                │");
            System.out.println("│  7. Exit & Save                         │");
            System.out.println("└─────────────────────────────────────────┘");
            System.out.print("Choose an option: ");
 
            int choice = getInt(sc);
            switch (choice) {
                case 1 -> viewAvailableRooms();
                case 2 -> makeReservation(sc);
                case 3 -> cancelReservation(sc);
                case 4 -> viewAllBookings();
                case 5 -> makePayment(sc);
                case 6 -> searchBooking(sc);
                case 7 -> { saveData(); running = false; System.out.println("Data saved. Goodbye! 🏨"); }
                default -> System.out.println("Invalid choice.");
            }
        }
        sc.close();
    }
 
    // View Available Rooms ---------------------------------------------------------------------------
    static void viewAvailableRooms() {
        System.out.println("\n🏨 AVAILABLE ROOMS");
        System.out.println("─".repeat(52));
        boolean any = false;
        for (Room r : rooms) {
            if (r.isAvailable) { System.out.println(r); any = true; }
        }
        if (!any) System.out.println("No rooms available at the moment.");
        printPriceSummary();
    }
 
    static void printPriceSummary() {
        System.out.println("\n💰 PRICE SUMMARY:");
        for (RoomType rt : RoomType.values())
            System.out.printf("  %-8s → $%.0f / night%n", rt.label, rt.pricePerNight);
    }
 
    // Make Reservation ------------------------------------------------------------------------------------
    static void makeReservation(Scanner sc) {
        viewAvailableRooms();
 
        System.out.print("\nEnter guest name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter phone number: ");
        String phone = sc.nextLine().trim();
 
        System.out.print("Enter room number: ");
        int roomNo = getInt(sc);
        Room room = findRoom(roomNo);
        if (room == null)        { System.out.println("Room not found.");      return; }
        if (!room.isAvailable)   { System.out.println("Room is already booked."); return; }
 
        System.out.print("Check-in date (YYYY-MM-DD): ");
        LocalDate checkIn = parseDate(sc.nextLine().trim());
        System.out.print("Check-out date (YYYY-MM-DD): ");
        LocalDate checkOut = parseDate(sc.nextLine().trim());
 
        if (checkIn == null || checkOut == null)  { System.out.println("Invalid date format."); return; }
        if (!checkOut.isAfter(checkIn))           { System.out.println("Check-out must be after check-in."); return; }
 
        Reservation res = new Reservation(name, phone, roomNo, room.type, checkIn, checkOut);
        reservations.add(res);
        room.isAvailable = false;
 
        res.printReceipt();
        System.out.println("✔ Reservation confirmed! Use option 5 to complete payment.");
    }
 
    // Cancel Reservation ----------------------------------------------------------------------------
    static void cancelReservation(Scanner sc) {
        System.out.print("Enter booking ID to cancel: ");
        int id = getInt(sc);
        Reservation res = findReservation(id);
        if (res == null) { System.out.println("Booking ID not found."); return; }
 
        Room room = findRoom(res.roomNumber);
        if (room != null) room.isAvailable = true;
        reservations.remove(res);
 
        System.out.println("✔ Reservation #" + id + " for " + res.guestName + " has been cancelled.");
        if (res.paid)
            System.out.printf("  Refund of $%.2f will be processed.%n", res.totalAmount);
    }
 
    // View All Bookings ----------------------------------------------------------------------------------
    static void viewAllBookings() {
        if (reservations.isEmpty()) { System.out.println("No bookings found."); return; }
        System.out.println("\n📋 ALL RESERVATIONS");
        System.out.println("─".repeat(72));
        System.out.printf("%-6s %-18s %-8s %-10s %-10s %7s %10s %-6s%n",
                "ID", "Guest", "Room", "Check-In", "Check-Out", "Nights", "Amount", "Paid");
        System.out.println("─".repeat(72));
        for (Reservation r : reservations) {
            System.out.printf("%-6d %-18s %-8d %-10s %-10s %7d %10.2f %-6s%n",
                    r.bookingId, r.guestName, r.roomNumber,
                    r.checkIn, r.checkOut, r.getNights(),
                    r.totalAmount, r.paid ? "✔ Yes" : "✘ No");
        }
    }
 
    // Payment -------------------------------------------------------------------------------------------------
    static void makePayment(Scanner sc) {
        System.out.print("Enter booking ID to pay: ");
        int id = getInt(sc);
        Reservation res = findReservation(id);
        if (res == null) { System.out.println("Booking not found."); return; }
        if (res.paid)    { System.out.println("Already paid."); return; }
 
        System.out.printf("Amount due: $%.2f%n", res.totalAmount);
        System.out.println("Payment methods: 1. Cash  2. Card  3. UPI");
        System.out.print("Choose payment method: ");
        int method = getInt(sc);
        String[] methods = {"Cash", "Card", "UPI"};
        String mLabel = (method >= 1 && method <= 3) ? methods[method - 1] : "Cash";
 
        res.paid = true;
        System.out.println("✔ Payment of $" + String.format("%.2f", res.totalAmount)
                + " received via " + mLabel + ". Thank you!");
        res.printReceipt();
    }
 
    // Search ----------------------------------------------------------------------------------------------------------
    static void searchBooking(Scanner sc) {
        System.out.print("Enter booking ID: ");
        int id = getInt(sc);
        Reservation res = findReservation(id);
        if (res == null) System.out.println("Booking not found.");
        else             res.printReceipt();
    }
 
    // Helpers -------------------------------------------------------------------------------------------------------
    static Room findRoom(int number) {
        for (Room r : rooms) if (r.roomNumber == number) return r;
        return null;
    }
 
    static Reservation findReservation(int id) {
        for (Reservation r : reservations) if (r.bookingId == id) return r;
        return null;
    }
 
    static LocalDate parseDate(String s) {
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }
 
    static int getInt(Scanner sc) {
        try { return Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
 
    // File I/O ------------------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    static void loadData() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            reservations = (List<Reservation>) ois.readObject();
            // Mark booked rooms
            for (Reservation r : reservations) {
                Room room = findRoom(r.roomNumber);
                if (room != null) room.isAvailable = false;
            }
            System.out.println("Bookings loaded from file (" + reservations.size() + " records).");
        } catch (Exception e) { System.out.println("Could not load data: " + e.getMessage()); }
    }
 
    static void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(reservations);
            System.out.println("Data saved to " + DATA_FILE);
        } catch (Exception e) { System.out.println("Save failed: " + e.getMessage()); }
    }
}
