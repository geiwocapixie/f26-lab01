package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.cmu.cs214.booking.domain.Booking;
import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import org.junit.jupiter.api.Test;

class IsAvailableTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");

    private BookingService svc;

    private void newService() {
        svc = new BookingService(new InMemoryBookingStore());
    }

    private Booking confirm(Room room, User user, TimeInterval interval) {
        return assertInstanceOf(BookingResult.Confirmed.class, svc.book(room, user, interval)).booking();
    }

    // 缺陷：已有预订 E 先开始、延伸进查询区间 Q，补丁只看 E.start 是否在 Q 内
    // E=[570,630), Q=[600,660) → 真实重叠 → 应返回 false；补丁返回 true（错）
    @Test
    void notAvailableWhenEarlierBookingRunsIntoTheRequest() {
        newService();
        confirm(roomA, alice, new TimeInterval(570, 630));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    // 缺陷补集：E 完全包含 Q 时也重叠，但 E.start < Q.start 同样被补丁漏判
    @Test
    void notAvailableWhenExistingBookingEnvelopsTheRequest() {
        newService();
        confirm(roomA, alice, new TimeInterval(540, 720));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    // 正常用例：E 完全在 Q 前且首尾相接 → 不重叠 → 可用
    @Test
    void availableWhenExistingBookingEndsRightAsRequestStarts() {
        newService();
        confirm(roomA, alice, new TimeInterval(540, 600));
        assertTrue(svc.isAvailable(roomA, new TimeInterval(600, 660)));
    }

    // 一致性：isAvailable 为 true 时 book 应得 Confirmed，为 false 时应得 Waitlisted
    @Test
    void isAvailableAgreesWithBook() {
        newService();
        confirm(roomA, alice, new TimeInterval(570, 630));
        TimeInterval request = new TimeInterval(600, 660);
        boolean available = svc.isAvailable(roomA, request);
        BookingResult result = svc.book(roomA, bob, request);
        assertEquals(available, result instanceof BookingResult.Confirmed);
    }
}
