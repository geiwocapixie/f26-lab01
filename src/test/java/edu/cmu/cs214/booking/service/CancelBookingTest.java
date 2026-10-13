package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // S3 晋升：取消后候补被晋升为已确认
    @Test
    void cancellingPromotesTheWaitlistedUser() {
        newService();
        Booking a = confirm(roomA, alice, new TimeInterval(600, 660));
        assertInstanceOf(BookingResult.Waitlisted.class, svc.book(roomA, bob, new TimeInterval(630, 700)));
        svc.cancelBooking(a.id());
        List<Booking> bookings = svc.listBookings(roomA);
        assertEquals(1, bookings.size());
        assertEquals("u2", bookings.get(0).user().id());
        assertEquals(new TimeInterval(630, 700), bookings.get(0).interval());
        assertTrue(store.waitlistForRoom(roomA).isEmpty());
    }

    // S4 先到先得 + 至多一人
    @Test
    void promotesEarliestFittingWaiterAndAtMostOne() {
        newService();
        Booking a = confirm(roomA, alice, new TimeInterval(600, 720));
        assertInstanceOf(BookingResult.Waitlisted.class, svc.book(roomA, bob, new TimeInterval(600, 660)));
        assertInstanceOf(BookingResult.Waitlisted.class, svc.book(roomA, carol, new TimeInterval(660, 700)));
        svc.cancelBooking(a.id());
        List<Booking> bookings = svc.listBookings(roomA);
        assertEquals(1, bookings.size());
        assertEquals("u2", bookings.get(0).user().id());
    }

    // S5 跳过仍冲突者，晋升下一个合适的
    @Test
    void skipsConflictingWaiterAndPromotesNextFittingOne() {
        newService();
        confirm(roomA, alice, new TimeInterval(600, 660));
        confirm(roomA, dave, new TimeInterval(700, 760));
        assertInstanceOf(BookingResult.Waitlisted.class, svc.book(roomA, bob, new TimeInterval(630, 730)));
        assertInstanceOf(BookingResult.Waitlisted.class, svc.book(roomA, carol, new TimeInterval(610, 650)));
        svc.cancelBooking(svc.listBookings(roomA).stream()
                .filter(b -> b.user().id().equals("u1")).findFirst().get().id());
        List<Booking> bookings = svc.listBookings(roomA);
        // Dave [700,760) 留着；Bob [630,730) 与 Dave 冲突→跳过；Carol [610,650) 合适→晋升
        assertEquals(2, bookings.size());
        assertTrue(bookings.stream().anyMatch(b -> b.user().id().equals("u3")));
    }

    // S6 不跨房间晋升
    @Test
    void doesNotPromoteAcrossRooms() {
        newService();
        Booking a = confirm(roomA, alice, new TimeInterval(600, 660));
        confirm(roomB, dave, new TimeInterval(600, 660));
        assertInstanceOf(BookingResult.Waitlisted.class, svc.book(roomB, bob, new TimeInterval(630, 700)));
        svc.cancelBooking(a.id());
        assertEquals(0, svc.listBookings(roomA).size());
        assertEquals(1, svc.listBookings(roomB).size());
        assertEquals(1, store.waitlistForRoom(roomB).size());
    }
}