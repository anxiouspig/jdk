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
     * ���µ�ǰ�̵߳ȴ���ֱ�������źŻ�{@linkplain Thread #interrupt interrupted}��
     *
     * <p>���{@code����}����������ԭ���ͷţ���ǰ�̳߳����̵߳��ȵ�Ŀ�ı����ã�
     * ����������״̬��ֱ�����������������֮һ<em>one</em>:
     * <ul>
     * <li>����һЩ�߳�Ϊ���{@code����}����{@link #signal}��������ǰ�̱߳�ѡ��ΪҪ���ѵ��߳�;
     * <li>����һЩ�̵߳���{@link #signalAll}�������������{@code����};
     * <li>�����߳�{@linkplain Thread #interrupt interrupts}��ǰ�̣߳�֧���ж��߳���ͣ;
     * <li>һ����< em >��ٻ���< / em >��������
     * </ul>
     *
     * <p>����������£��ڴ˷�������֮ǰ����ǰ�̱߳������»�ȡ�����������������
     * ���̷߳���ʱ��<em>��֤</em>�����������
     *
     * <p>�����ǰ�̣߳�
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     * <li>�Ƿ�֧��{@linkplain Thread#interrupt}�ڵȴ����ж��߳���ͣʱ��
     * </ul>
     * Ȼ���׳�{@link InterruptedException}���������ǰ�̵߳��ж�״̬��
     * �ڵ�һ������£�û��ָ���Ƿ����ͷ���֮ǰ�����жϲ��ԡ�
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>�ٶ���ǰ�߳��ڵ��ô˷���ʱ�������{@code����}������������ʵ����ȷ������Ƿ���ˣ�
     * ������ǣ��������Ӧ��ͨ�������׳�һ���쳣(����{@link IllegalMonitorStateException})��
     * ʵ�ֱ����¼�����ʵ��
     *
     * <p>ʵ�ֿ��ܸ���������Ӧ�жϣ���������Ӧ�źŵ������������ء�
     * ����������£�ʵ�ֱ���ȷ���źű��ض�����һ���ȴ����߳�(����еĻ�)��
     *
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    void await() throws InterruptedException;

    /**
     * ���µ�ǰ�̵߳ȴ���ֱ������֪ͨ��
     *
     * <p>���������ص�����ԭ���ͷţ���ǰ�̳߳����̵߳��ȵ�Ŀ�ı����ã�����������״̬��
     * ֱ�����������������֮һ<em>one</em>:
     * <ul>
     * <li>����һЩ�߳�Ϊ���{@code����}����{@link #signal}��������ǰ�̱߳�ѡ��ΪҪ���ѵ��߳�;
     * <li>S����һЩ�̵߳���{@link #signalAll}�������������{@code����};
     * <li>һ����< em >��ٻ���< / em >��������
     * </ul>
     *
     * <p>����������£��ڴ˷�������֮ǰ����ǰ�̱߳������»�ȡ�����������������
     * ���̷߳���ʱ��<em>��֤</em>�����������
     *
     * <p>�����ǰ�̵߳��ж�״̬�����������������ʱ���õģ���������{@linkplain Thread #interrupt interrupted}��
     * ��ô�ڵȴ�ʱ�����������ȴ���ֱ�������źš��������մ������������ʱ�������ж�״̬��Ȼ�ᱻ���á�
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>�ٶ���ǰ�߳��ڵ��ô˷���ʱ�������{@code����}������������ʵ����ȷ������Ƿ���ˣ�������ǣ��������Ӧ��
     * ͨ�������׳�һ���쳣(����{@link IllegalMonitorStateException})��ʵ�ֱ����¼�����ʵ��
     */
    void awaitUninterruptibly();

    /**
     * ���µ�ǰ�̵߳ȴ���ֱ�������źŻ��ж�������ָ���ĵȴ�ʱ����ڡ�
     *
     * <p>���������ص�����ԭ���ͷţ���ǰ�̳߳����̵߳��ȵ�Ŀ�ı����ã�����������״̬��ֱ��������������е�һ��<em> </em>:
     * <ul>
     * <li>����һЩ�߳�Ϊ���{@code����}����{@link #signal}��������ǰ�̱߳�ѡ��ΪҪ���ѵ��߳�;
     * <li>����һЩ�̵߳���{@link #signalAll}�������������{@code����};
     * <li>�����߳�{@linkplain thread #interrupt interrupts}��ǰ�̣߳�֧���ж��߳���ͣ;��<li>ָ���ĵȴ�ʱ���ѹ�;
     * <li>һ����< em >��ٻ���< / em >��������
     * </ul>
     *
     * <p>����������£��ڴ˷�������֮ǰ����ǰ�̱߳������»�ȡ��������������������̷߳���ʱ��<em>��֤</em>�����������
     *
     * <p>�����ǰ�߳�:
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;��
     * <li>Ϊ{@linkplain Thread#interrupt}���ڵȴ�ʱ��֧���ж��߳���ͣ��
     * </ul>
     * Ȼ���׳�{@link InterruptedException}���������ǰ�̵߳��ж�״̬���ڵ�һ������£�û��ָ���Ƿ����ͷ���֮ǰ�����жϲ��ԡ�
     *
     * <p>�÷������ݷ���ʱ�ṩ��{@code nanosTimeout}ֵ����ʣ��ȴ����������Ĺ���ֵ�������ʱ���򷵻�С�ڻ�������ֵ����ֵ������ȷ���ڵȴ����ص��ȴ�������Ȼ��Ч��������Ƿ���Ҫ���µȴ����Լ���Ҫ�೤ʱ�����µȴ������ַ����ĵ�����;����:
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
     * <p>���˵��:���������Ҫһ������������Ա����ڱ���ʣ��ʱ��ʱ���ֽضϴ������־�����ʧ��ʹ����Ա����ȷ���ܵȴ�ʱ�䲻������µȴ�ʱָ����ϵͳʱ��̡�
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>�ٶ���ǰ�߳��ڵ��ô˷���ʱ�������{@code����}������������ʵ����ȷ������Ƿ���ˣ�������ǣ��������Ӧ��ͨ�������׳�һ���쳣(����{@link IllegalMonitorStateException})��ʵ�ֱ����¼�����ʵ��
     *
     * <p>ʵ�ֿ��ܸ���������Ӧ�ж϶�������Ӧ�źŵ������������أ����߸�������ָʾָ���ȴ�ʱ������š�������������£�ʵ�ֶ�����ȷ���źű��ض�����һ���ȴ����߳�(����еĻ�)��
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
     * ���µ�ǰ�̵߳ȴ���ֱ�������źŻ��ж�������ָ���ĵȴ�ʱ����ڡ������������Ϊ�ϵȼ���:
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
     * ���µ�ǰ�̵߳ȴ���ֱ�������źŻ��ж�������ָ���Ľ�ֹ���ڹ��ڡ�
     *
     * <p>���������ص�����ԭ���ͷţ���ǰ�̳߳����̵߳��ȵ�Ŀ�ı����ã�����������״̬��
     * ֱ��������������е�һ��<em> </em>:
     * <ul>
     * <li>����һЩ�߳�Ϊ���{@code����}����{@link #signal}��������ǰ�̱߳�ѡ��ΪҪ���ѵ��߳�;
     * <li>����һЩ�̵߳���{@link #signalAll}�������������{@code����};
     * <li>�����߳�{@linkplain thread #interrupt interrupts}��ǰ�̣߳�֧���ж��߳���ͣ;��<li>ָ�������ѹ�;
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>����������£��ڴ˷�������֮ǰ����ǰ�̱߳������»�ȡ�����������������
     * ���̷߳���ʱ��<em>��֤</em>�����������
     *
     *
     * <p>�����ǰ�߳�:
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;��
     * <li>Ϊ{@linkplain Thread#interrupt}���ڵȴ�ʱ��֧���ж��߳���ͣ��
     * </ul>
     * Ȼ���׳�{@link InterruptedException}���������ǰ�̵߳��ж�״̬���ڵ�һ������£�û��ָ���Ƿ����ͷ���֮ǰ�����жϲ��ԡ�
     *
     *
     * <p>����ֵָʾ��ֹ�����Ƿ��Ѿ����ˣ����÷�����
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
     * <p>�ٶ���ǰ�߳��ڵ��ô˷���ʱ�������{@code����}����������
     * ��ʵ����ȷ������Ƿ���ˣ�������ǣ��������Ӧ��ͨ�������׳�һ���쳣(����
     * {@link IllegalMonitorStateException})��ʵ�ֱ����¼�����ʵ��
     *
     * <p>ʵ�ֿ��ܸ���������Ӧ�ж϶�������Ӧ�źŵ������������أ�
     * ���߸�������ָʾָ���Ľ�ֹ���ڵ�ͨ����������������£�
     * ʵ�ֶ�����ȷ���źű��ض�����һ���ȴ����߳�(����еĻ�)��
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * ����һ�����ڵȴ����̡߳�
     *
     * <p>������κ��߳��ڴ������µȴ�����ѡ��һ���߳̽��л��ѡ�
     * Ȼ�󣬸��̱߳����ڴ�{@code await}����֮ǰ���»�ȡ����
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>ʵ�ֿ���(ͨ��������)Ҫ��ǰ�߳��ڵ��ô˷���ʱ�������{@code����}����������
     * ʵ�ֱ����¼��ǰ����������δ������ʱ����ȡ���κβ�����ͨ�����׳�һ���쳣������
     * {@link IllegalMonitorStateException}��
     */
    void signal();

    /**
     * �������еȴ����̡߳�
     *
     * <p>������κ��߳��ڴ������µȴ�����ô���Ƕ��������ѡ�ÿ���߳��ڴ�{@code await}����֮ǰ�������»�ȡ����
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>ʵ�ֿ���(ͨ��������)Ҫ��ǰ�߳��ڵ��ô˷���ʱ�������{@code����}����������
     * ʵ�ֱ����¼��ǰ����������δ������ʱ����ȡ���κβ�����ͨ�����׳�һ���쳣������
     * {@link IllegalMonitorStateException}��
     */
    void signalAll();
}
