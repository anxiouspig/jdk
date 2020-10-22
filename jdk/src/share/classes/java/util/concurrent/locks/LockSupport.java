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
import sun.misc.Unsafe;

/**
 * ������������ͬ����Ļ����߳�����ԭ�
 *
 * �������ʹ������ÿ���̹߳���һ�����֤(���ź������������)��
 * ���������֤������park���������أ����ڴ˹��������ĵ���;����������������
 * �����δ������֤�������unpark��ʹ���֤���á�(���ź�����ͬ�����֤�����ۻ������ֻ��һ����)
 *
 * ����park��unpark�ṩ����Ч�������ͷ������̵߳ķ�������Щ���������������·��������̵߳�����
 * s Thread.suspend and Thread.resume:һ���̵߳���park����һ���߳�unpark�������̻߳��ԣ�
 * �������֤�����⣬��������ߵ��̱߳��жϣ�park�����أ�����֧�ֳ�ʱ�汾��
 * park����Ҳ�������κ�����ʱ�䷵�أ���Ϊ��û��ԭ�򡱣�����ͨ�������ڷ���ʱ��ѭ�������¼��������
 * ����������ϣ�park��Ϊһ������æ�ĵȴ������Ż������˷�̫���ʱ����ת�����������unpark����Ч�ġ�
 *
 * park��������ʽҲ��֧��blocker�������������������̱߳�����ʱ���м�¼��
 * ��������Ӻ���Ϲ���ʶ���̱߳�������ԭ��(��Щ���߿��ܻ�ʹ��getBlocker(Thread)����������������)
 * ǿ�ҽ���ʹ����Щ��������û�д˲�����ԭʼ��������ʵ������Ϊ�������ṩ�����������������ġ�
 *
 *  <pre> {@code
 * while (!canProceed()) { ... LockSupport.park(this); }}</pre>
 *
 * �ڵ���park֮ǰ���Ȳ��ܼ���������Ҳ����ִ���κ������������Ӷ�������������������Ϊÿ���߳�ֻ��һ�����֤.
 *
 * <p><b>Sample Usage.</b> Here is a sketch of a first-in-first-out
 * non-reentrant lock class:
 *  <pre> {@code
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue<Thread> waiters
 *     = new ConcurrentLinkedQueue<Thread>();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != current ||
 *            !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     if (wasInterrupted)          // reassert interrupt status on exit
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 * }}</pre>
 */
public class LockSupport {
    private LockSupport() {} // Cannot be instantiated.

    // �÷�������Thread��Ԥ���ֶΣ���ϵĶ���
    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * �ṩ�����̵߳����֤(�������û�п���)������߳���park�ϱ���������ô����������
     * ����������һ������park�Ǳ�֤�����������û�������������̣߳����ܱ�֤�˲������κ�Ч����
     *
     * @param thread Ҫunpark���̣߳���null������������£��˲�����Ч
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }

    /**
     * Ϊ�̵߳��ȵ�Ŀ�Ľ��õ�ǰ�̣߳��������֤���á�
     *
     * <p>������֤���ã���ʹ�����֤����������;���򣬵�ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ������ã�����������״̬��ֱ�����������������֮һ:
     *
     * <ul>
     * <li>����һЩ�߳��Ե�ǰ�߳�ΪĿ�����unpark;
     *
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     *
     * <li>��ٵĵ���(Ҳ����˵��û���κ�����)���ء�
     * </ul>
     *
     * <p>�˷�������������Щԭ���¸÷������ء�������Ӧ�����¼�鵼���߳�����ֹͣ��������
     * ���磬�����߻�����ȷ���̷߳���ʱ���ж�״̬��
     *
     * @param blocker ������߳�ֹͣ��ͬ������
     * @since 1.6
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }

    /**
     * ���������֤��������ָ���ĵȴ�ʱ���ڣ���ֹ��ǰ�߳������̵߳��ȡ�
     *
     * <p>������֤���ã���ʹ�����֤����������;���򣬵�ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ������ã�
     * ����������״̬��ֱ�����������������֮һ:
     *
     * <ul>
     * <li>����һЩ�߳��Ե�ǰ�߳�ΪĿ�����unpark;
     *
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     *
     * <li>ָ���ĵȺ�ʱ���ѹ�;
     *
     * <li>��ٵĵ���(Ҳ����˵��û���κ�����)���ء�
     * </ul>
     *
     * <p>�˷�������������Щԭ���¸÷������ء�������Ӧ�����¼�鵼���߳�����ֹͣ��������
     * ���磬�����߻�����ȷ���̵߳��ж�״̬�򷵻�ʱ������ʱ�䡣
     *
     * @param blocker ������߳�ֹͣ��ͬ������
     * @param nanos �ȴ������������
     * @since 1.6
     */
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }

    /**
     * Ϊ���̵߳��ȵ�Ŀ�ģ���ָ���Ľ�ֹ����֮ǰ���õ�ǰ�̣߳��������֤���á�
     *
     * <p>������֤���ã���ʹ�����֤����������;
     * ���򣬵�ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ������ã�����������״̬��ֱ�����������������֮һ:
     *
     * <ul>
     * <li>����һЩ�߳��Ե�ǰ�߳�ΪĿ�����unpark
     *
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     *
     * <li>����ָ������;
     *
     * <li>��ٵĵ���(Ҳ����˵��û���κ�����)���ء�
     * </ul>
     *
     * <p>�˷�������������Щԭ���¸÷������ء�������Ӧ�����¼�鵼���߳�����ֹͣ��������
     * ���磬�����߻�����ȷ���̵߳��ж�״̬���򷵻�ʱ�ĵ�ǰʱ�䡣
     *
     * @param blocker ������߳�ֹͣ��ͬ������
     * @param deadline �ȴ�ʱ��ľ���ʱ��(�Ժ���Ϊ��λ)
     * @since 1.6
     */
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * �����ṩ��park���������һ�ε��õ�blocker���󣬸÷�����δ��������������δ���������򷵻�null��
     * ���ص�ֵֻ��һ��˲ʱ���ա����߳̿����ڲ�ͬ��blocker�����Ͻ����������������
     *
     * @param t the thread
     * @return the blocker
     * @throws NullPointerException if argument is null
     * @since 1.6
     */
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * Ϊ�̵߳��ȵ�Ŀ�Ľ��õ�ǰ�̣߳��������֤���á�
     *
     * <p>������֤���ã���ʹ�����֤����������;���򣬵�ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ������ã�
     * ����������״̬��ֱ�����������������֮һ:
     *
     * <ul>
     *
     * <li>����һЩ�߳��Ե�ǰ�߳�ΪĿ�����unpark;
     *
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     *
     * <li>��ٵĵ���(Ҳ����˵��û���κ�����)���ء�
     * </ul>
     *
     * <p>�˷�������������Щԭ���¸÷������ء�������Ӧ�����¼�鵼���߳�����ֹͣ��������
     * ���磬�����߻�����ȷ���̷߳���ʱ���ж�״̬��
     */
    public static void park() {
        UNSAFE.park(false, 0L);
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }

    /**
     * Ϊ���̵߳��ȵ�Ŀ�ģ���ָ���Ľ�ֹ����֮ǰ���õ�ǰ�̣߳��������֤���á�
     *
     * <p>������֤���ã���ʹ�����֤����������;���򣬵�ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ������ã�
     * ����������״̬��ֱ�����������������֮һ:
     *
     * <ul>
     * <li>����һЩ�߳��Ե�ǰ�߳�ΪĿ�����unpark;
     *
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     *
     * <li>����ָ������;
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>�˷�������������Щԭ���¸÷������ء�������Ӧ�����¼�鵼���߳�����ֹͣ��������
     * ���磬�����߻�����ȷ���̵߳��ж�״̬���򷵻�ʱ�ĵ�ǰʱ�䡣
     *
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     */
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }

    /**
     * ����α�����ʼ������µĸ������ӡ����ڰ��������ƣ���ThreadLocalRandom���ơ�
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }

}
