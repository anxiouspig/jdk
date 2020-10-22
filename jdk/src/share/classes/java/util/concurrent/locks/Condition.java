/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * {@code Condition} factors out the {@code Object} monitor
 * methods ({@link Object#wait() wait}, {@link Object#notify notify}
 * and {@link Object#notifyAll notifyAll}) into distinct objects to
 * give the effect of having multiple wait-sets per object, by
 * combining them with the use of arbitrary {@link Lock} implementations.
 * Where a {@code Lock} replaces the use of {@code synchronized} methods
 * and statements, a {@code Condition} replaces the use of the Object
 * monitor methods.
 *
 * <p>Conditions (also known as <em>condition queues</em> or
 * <em>condition variables</em>) provide a means for one thread to
 * suspend execution (to &quot;wait&quot;) until notified by another
 * thread that some state condition may now be true.  Because access
 * to this shared state information occurs in different threads, it
 * must be protected, so a lock of some form is associated with the
 * condition. The key property that waiting for a condition provides
 * is that it <em>atomically</em> releases the associated lock and
 * suspends the current thread, just like {@code Object.wait}.
 *
 * <p>A {@code Condition} instance is intrinsically bound to a lock.
 * To obtain a {@code Condition} instance for a particular {@link Lock}
 * instance use its {@link Lock#newCondition newCondition()} method.
 *
 * <p>As an example, suppose we have a bounded buffer which supports
 * {@code put} and {@code take} methods.  If a
 * {@code take} is attempted on an empty buffer, then the thread will block
 * until an item becomes available; if a {@code put} is attempted on a
 * full buffer, then the thread will block until a space becomes available.
 * We would like to keep waiting {@code put} threads and {@code take}
 * threads in separate wait-sets so that we can use the optimization of
 * only notifying a single thread at a time when items or spaces become
 * available in the buffer. This can be achieved using two
 * {@link Condition} instances.
 * <pre>
 * class BoundedBuffer {
 *   <b>final Lock lock = new ReentrantLock();</b>
 *   final Condition notFull  = <b>lock.newCondition(); </b>
 *   final Condition notEmpty = <b>lock.newCondition(); </b>
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(Object x) throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == items.length)
 *         <b>notFull.await();</b>
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       <b>notEmpty.signal();</b>
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 *
 *   public Object take() throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == 0)
 *         <b>notEmpty.await();</b>
 *       Object x = items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       <b>notFull.signal();</b>
 *       return x;
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 * }
 * </pre>
 *
 * (The {@link java.util.concurrent.ArrayBlockingQueue} class provides
 * this functionality, so there is no reason to implement this
 * sample usage class.)
 *
 * <p>A {@code Condition} implementation can provide behavior and semantics
 * that is
 * different from that of the {@code Object} monitor methods, such as
 * guaranteed ordering for notifications, or not requiring a lock to be held
 * when performing notifications.
 * If an implementation provides such specialized semantics then the
 * implementation must document those semantics.
 *
 * <p>Note that {@code Condition} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement,
 * and can have their own monitor {@link Object#wait wait} and
 * {@link Object#notify notification} methods invoked.
 * Acquiring the monitor lock of a {@code Condition} instance, or using its
 * monitor methods, has no specified relationship with acquiring the
 * {@link Lock} associated with that {@code Condition} or the use of its
 * {@linkplain #await waiting} and {@linkplain #signal signalling} methods.
 * It is recommended that to avoid confusion you never use {@code Condition}
 * instances in this way, except perhaps within their own implementation.
 *
 * <p>Except where noted, passing a {@code null} value for any parameter
 * will result in a {@link NullPointerException} being thrown.
 *
 * <h3>Implementation Considerations</h3>
 *
 * <p>When waiting upon a {@code Condition}, a &quot;<em>spurious
 * wakeup</em>&quot; is permitted to occur, in
 * general, as a concession to the underlying platform semantics.
 * This has little practical impact on most application programs as a
 * {@code Condition} should always be waited upon in a loop, testing
 * the state predicate that is being waited for.  An implementation is
 * free to remove the possibility of spurious wakeups but it is
 * recommended that applications programmers always assume that they can
 * occur and so always wait in a loop.
 *
 * <p>The three forms of condition waiting
 * (interruptible, non-interruptible, and timed) may differ in their ease of
 * implementation on some platforms and in their performance characteristics.
 * In particular, it may be difficult to provide these features and maintain
 * specific semantics such as ordering guarantees.
 * Further, the ability to interrupt the actual suspension of the thread may
 * not always be feasible to implement on all platforms.
 *
 * <p>Consequently, an implementation is not required to define exactly the
 * same guarantees or semantics for all three forms of waiting, nor is it
 * required to support interruption of the actual suspension of the thread.
 *
 * <p>An implementation is required to
 * clearly document the semantics and guarantees provided by each of the
 * waiting methods, and when an implementation does support interruption of
 * thread suspension then it must obey the interruption semantics as defined
 * in this interface.
 *
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action that may have
 * unblocked the thread. An implementation should document this behavior.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Condition {

    /**
     * 导致当前线程等待，直到发出信号或{@linkplain Thread #interrupt interrupted}。
     *
     * <p>与此{@code条件}关联的锁被原子释放，当前线程出于线程调度的目的被禁用，
     * 并处于休眠状态，直到发生以下四种情况之一<em>one</em>:
     * <ul>
     * <li>其他一些线程为这个{@code条件}调用{@link #signal}方法，当前线程被选择为要唤醒的线程;
     * <li>其他一些线程调用{@link #signalAll}方法来处理这个{@code条件};
     * <li>其他线程{@linkplain Thread #interrupt interrupts}当前线程，支持中断线程暂停;
     * <li>一个“< em >虚假唤醒< / em >“发生。
     * </ul>
     *
     * <p>在所有情况下，在此方法返回之前，当前线程必须重新获取与此条件关联的锁。
     * 当线程返回时，<em>保证</em>持有这个锁。
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置其中断状态;
     * <li>是否支持{@linkplain Thread#interrupt}在等待和中断线程暂停时，
     * </ul>
     * 然后抛出{@link InterruptedException}，并清除当前线程的中断状态。
     * 在第一种情况下，没有指定是否在释放锁之前进行中断测试。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>假定当前线程在调用此方法时持有与此{@code条件}关联的锁。由实现来确定情况是否如此，
     * 如果不是，则如何响应。通常，会抛出一个异常(例如{@link IllegalMonitorStateException})，
     * 实现必须记录这个事实。
     *
     * <p>实现可能更倾向于响应中断，而不是响应信号的正常方法返回。
     * 在这种情况下，实现必须确保信号被重定向到另一个等待的线程(如果有的话)。
     *
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    void await() throws InterruptedException;

    /**
     * 导致当前线程等待，直到它被通知。
     *
     * <p>与此条件相关的锁被原子释放，当前线程出于线程调度的目的被禁用，并处于休眠状态，
     * 直到发生以下三种情况之一<em>one</em>:
     * <ul>
     * <li>其他一些线程为这个{@code条件}调用{@link #signal}方法，当前线程被选择为要唤醒的线程;
     * <li>S其他一些线程调用{@link #signalAll}方法来处理这个{@code条件};
     * <li>一个“< em >虚假唤醒< / em >“发生。
     * </ul>
     *
     * <p>在所有情况下，在此方法返回之前，当前线程必须重新获取与此条件关联的锁。
     * 当线程返回时，<em>保证</em>持有这个锁。
     *
     * <p>如果当前线程的中断状态是在它进入这个方法时设置的，或者它是{@linkplain Thread #interrupt interrupted}，
     * 那么在等待时，它将继续等待，直到发出信号。当它最终从这个方法返回时，它的中断状态仍然会被设置。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>假定当前线程在调用此方法时持有与此{@code条件}关联的锁。由实现来确定情况是否如此，如果不是，则如何响应。
     * 通常，会抛出一个异常(例如{@link IllegalMonitorStateException})，实现必须记录这个事实。
     */
    void awaitUninterruptibly();

    /**
     * 导致当前线程等待，直到发出信号或中断它，或指定的等待时间过期。
     *
     * <p>与此条件相关的锁被原子释放，当前线程出于线程调度的目的被禁用，并处于休眠状态，直到发生五件事情中的一件<em> </em>:
     * <ul>
     * <li>其他一些线程为这个{@code条件}调用{@link #signal}方法，当前线程被选择为要唤醒的线程;
     * <li>其他一些线程调用{@link #signalAll}方法来处理这个{@code条件};
     * <li>其他线程{@linkplain thread #interrupt interrupts}当前线程，支持中断线程暂停;或<li>指定的等待时间已过;
     * <li>一个“< em >虚假唤醒< / em >“发生。
     * </ul>
     *
     * <p>在所有情况下，在此方法返回之前，当前线程必须重新获取与此条件关联的锁。当线程返回时，<em>保证</em>持有这个锁。
     *
     * <p>如果当前线程:
     * <ul>
     * <li>在进入此方法时已设置其中断状态;或
     * <li>为{@linkplain Thread#interrupt}，在等待时，支持中断线程暂停，
     * </ul>
     * 然后抛出{@link InterruptedException}，并清除当前线程的中断状态。在第一种情况下，没有指定是否在释放锁之前进行中断测试。
     *
     * <p>该方法根据返回时提供的{@code nanosTimeout}值返回剩余等待的纳秒数的估计值，如果超时，则返回小于或等于零的值。此值可用于确定在等待返回但等待条件仍然无效的情况下是否需要重新等待，以及需要多长时间重新等待。这种方法的典型用途如下:
     *
     *  <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p>设计说明:这个方法需要一个纳秒参数，以避免在报告剩余时间时出现截断错误。这种精度损失将使程序员难以确保总等待时间不会比重新等待时指定的系统时间短。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>假定当前线程在调用此方法时持有与此{@code条件}关联的锁。由实现来确定情况是否如此，如果不是，则如何响应。通常，会抛出一个异常(例如{@link IllegalMonitorStateException})，实现必须记录这个事实。
     *
     * <p>实现可能更倾向于响应中断而不是响应信号的正常方法返回，或者更倾向于指示指定等待时间的流逝。在这两种情况下，实现都必须确保信号被重定向到另一个等待的线程(如果有的话)。
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     * @return an estimate of the {@code nanosTimeout} value minus
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * 导致当前线程等待，直到发出信号或中断它，或指定的等待时间过期。这个方法在行为上等价于:
     *  <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>
     *
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 导致当前线程等待，直到发出信号或中断它，或指定的截止日期过期。
     *
     * <p>与此条件相关的锁被原子释放，当前线程出于线程调度的目的被禁用，并处于休眠状态，
     * 直到发生五件事情中的一件<em> </em>:
     * <ul>
     * <li>其他一些线程为这个{@code条件}调用{@link #signal}方法，当前线程被选择为要唤醒的线程;
     * <li>其他一些线程调用{@link #signalAll}方法来处理这个{@code条件};
     * <li>其他线程{@linkplain thread #interrupt interrupts}当前线程，支持中断线程暂停;或<li>指定期限已过;
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>在所有情况下，在此方法返回之前，当前线程必须重新获取与此条件关联的锁。
     * 当线程返回时，<em>保证</em>持有这个锁。
     *
     *
     * <p>如果当前线程:
     * <ul>
     * <li>在进入此方法时已设置其中断状态;或
     * <li>为{@linkplain Thread#interrupt}，在等待时，支持中断线程暂停，
     * </ul>
     * 然后抛出{@link InterruptedException}，并清除当前线程的中断状态。在第一种情况下，没有指定是否在释放锁之前进行中断测试。
     *
     *
     * <p>返回值指示截止日期是否已经过了，其用法如下
     *  <pre> {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>假定当前线程在调用此方法时持有与此{@code条件}关联的锁。
     * 由实现来确定情况是否如此，如果不是，则如何响应。通常，会抛出一个异常(例如
     * {@link IllegalMonitorStateException})，实现必须记录这个事实。
     *
     * <p>实现可能更倾向于响应中断而不是响应信号的正常方法返回，
     * 或者更倾向于指示指定的截止日期的通过。在这两种情况下，
     * 实现都必须确保信号被重定向到另一个等待的线程(如果有的话)。
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * 唤醒一个正在等待的线程。
     *
     * <p>如果有任何线程在此条件下等待，则选择一个线程进行唤醒。
     * 然后，该线程必须在从{@code await}返回之前重新获取锁。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>实现可能(通常是这样)要求当前线程在调用此方法时持有与此{@code条件}关联的锁。
     * 实现必须记录此前提条件和在未持有锁时所采取的任何操作。通常会抛出一个异常，比如
     * {@link IllegalMonitorStateException}。
     */
    void signal();

    /**
     * 唤醒所有等待的线程。
     *
     * <p>如果有任何线程在此条件下等待，那么它们都将被唤醒。每个线程在从{@code await}返回之前必须重新获取锁。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>实现可能(通常是这样)要求当前线程在调用此方法时持有与此{@code条件}关联的锁。
     * 实现必须记录此前提条件和在未持有锁时所采取的任何操作。通常会抛出一个异常，比如
     * {@link IllegalMonitorStateException}。
     */
    void signalAll();
}
