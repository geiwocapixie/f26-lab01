package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import edu.cmu.cs214.booking.domain.Booking;
import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import java.util.List;
import org.junit.jupiter.api.Test;

class CancelBookingTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final Room roomB = new Room("B", "Beta", 4);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");
    private final User carol = new User("u3", "Carol");
    private final User dave = new User("u4", "Dave");

    private InMemoryBookingStore store;
    private BookingService svc;

    private void newService() {
        store = new InMemoryBookingStore();
        svc = new BookingService(store);
    }

    private Booking confirm(Room room, User user, TimeInterval interval) {
        return assertInstanceOf(BookingResult.Confirmed.class, svc.book(room, user, interval)).booking();
    }

    // S1 基本取消
    @Test
    void cancellingRemovesTheBooking() {
        newService();
        Booking a = confirm(roomA, alice, new TimeInterval(600, 660));
        svc.cancelBooking(a.id());
        assertEquals(0, svc.listBookings(roomA).size());
    }

    // S2 无效 id 不做事
    @Test
    void cancellingUnknownIdDoesNothing() {
        newService();
        confirm(roomA, alice, new TimeInterval(600, 660));
        svc.cancelBooking("no-such-id");
        assertEquals(1, svc.listBookings(roomA).size());
    }
}