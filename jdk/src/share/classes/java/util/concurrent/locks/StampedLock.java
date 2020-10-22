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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.LockSupport;

/**
 * һ�ֻ�������������������ģʽ���ڿ��ƶ�/д���ʡ�StampedLock��״̬�ɰ汾��ģʽ��ɡ�
 * ����ȡ��������һ����ʾ�����ƶ���״̬�ķ��ʵĴ���;��Щ�����ġ����ԡ��汾���ܻ᷵�������ֵ0����ʾ����ʧ�ܡ�
 * ���ͷź�ת��������Ҫ������Ϊ�������������������״̬��ƥ�䣬���ʧ�ܡ�����ģʽ��:
 *
 * <ul>
 *
 *  <li>Write:����writeLock()���������ȴ���ռ���ʣ�����һ�������ڷ���unlockWrite(long)��ʹ�õĴ������ͷ�����
 *  ���ṩ�˲���ʱ�Ͷ�ʱ��tryWriteLock�汾����������дģʽʱ�����ܲ����ö����������ֹ۶���֤����ʧ�ܡ�</li>
 *
 *  <li>Reading:����readLock()���������ȴ��Ƕ�ռ���ʣ�����һ�������ڷ���unlockRead(long)��ʹ�õĴ������ͷ�����
 *  ���ṩ�˲���ʱ�Ͷ�ʱ��tryReadLock�汾��</li>
 *
 *  <li>Optimistic Reading:����tryOptimisticRead()���ڵ�ǰ��δ����дģʽʱ�ŷ��ط�����ǡ�
 *  ����validate(long)����true������ڻ�ȡ�����Ĵ���֮������û����дģʽ�»�á�
 *  ����ģʽ���Ա���Ϊ��һ���ǳ����Ķ����汾�����Ա�д�������κ�ʱ����ơ�
 *  ���ڶ̵�ֻ�������ʹ���ֹ�ģʽͨ�����Լ������ò������������Ȼ��������ʹ�ñ����Ǵ����ġ�
 *  �ֹ۶�ȡ����Ӧ��ֻ��ȡ�ֶβ������Ǳ����ھֲ������У��Ա�����֤֮��ʹ�á�
 *  ���ֹ�ģʽ�¶�ȡ���ֶο��ܷǳ���һ�£�
 *  ���ֻ�������㹻��Ϥ���ݱ�ʾ�Լ��һ���Ժ�/�򷴸����÷���validate()ʱ�Ż�ʹ�á�
 *  ���磬�����ȶ�ȡ������������ã�Ȼ���������һ���ֶΡ�Ԫ�ػ򷽷�ʱ��ͨ����Ҫ��Щ���衣</li>
 *
 * </ul>
 *
 * <p>���໹֧���������ؿ�����ģʽ�ṩת���ķ�����
 * ���磬����tryConvertToWriteLock(long)���ԡ�������һ��ģʽ��
 * ���(1)�Ѿ�����д��ģʽ(2)���ڶ�ȡģʽ������û��������ȡ����
 * ����(3)�����ֹ�ģʽ���������ã��򷵻�һ����Ч��д����
 * ��Щ��������ʽּ�ڰ��������ڻ��ڻ��˵�����г��ֵ�һЩ�������͡�
 *
 * <p>�ڿ����̰߳�ȫ���ʱ��StampedLocks�����Ϊ�ڲ�ʵ�ó���
 * ���ǵ�ʹ�����������������������ݡ�����ͷ������ڲ����Ե�֪ʶ��
 * �����ǲ�������ģ�����������ʵ�岻Ӧ�õ�������δ֪�ķ�����
 * ��Щ����������ͼ���»�ȡ��(���������Խ����Ǵ��ݸ���������ʹ�û�ת�����ķ���)��
 * ������ģʽ��ʹ����������صĴ��벿�����޸����õġ�
 * δ����֤���ֹ۶�ȡ���ֲ��ܵ��ò�֪����������Ǳ�ڲ�һ���Եķ�����
 * ����ʹ�����޵ı�ʾ���������������ǲ���ȫ��(���硣����Ч��ӡ�¿����ǿɲ²��)��
 * ��Ʊ��ֵ����(������)��������һ���ѭ��ʹ�á�δ��ʹ�û���֤�����г��������޵Ĵ��ǿ����޷���ȷ��֤��
 * StampedLocks�ǿ����л��ģ������Ƿ����л�Ϊ��ʼ����״̬��������Ƕ���Զ������û���ô���
 *
 * <p>StampedLock�ĵ��Ȳ��Բ�����������ѡ���д������֮��Ȼ��
 * ���еġ����ԡ��������Ǿ������Ŭ���ģ���һ�������κε��Ȼ�ƽ���ԡ�
 * �ӻ�ȡ��ת�������κΡ����ԡ��������ص��㲻��Я���й���״̬���κ���Ϣ;�������ÿ��ܳɹ���
 *
 * <p>��Ϊ��֧�ֿ�����ģʽ��Э��ʹ�ã���������಻ֱ��ʵ�������д���ӿڡ�
 * ���ǣ���ֻ��Ҫ��ع��ܼ���Ӧ�ó����У����Խ�StampedLock��ΪasReadLock()��
 * asWriteLock()��asReadWriteLock()��
 *
 * <p>����˵����ά���򵥶�ά������е�һЩ�÷���ʾ��������ʾ��һЩtry/catchԼ����
 * �������ﲢ���ϸ���Ҫ���ǣ���Ϊ���ǵ������в����ܳ����쳣��<br>
 *
 *  <pre>{@code
 * class Point {
 *   private double x, y;
 *   private final StampedLock sl = new StampedLock();
 *
 *   void move(double deltaX, double deltaY) { // һ������������
 *     long stamp = sl.writeLock();
 *     try {
 *       x += deltaX;
 *       y += deltaY;
 *     } finally {
 *       sl.unlockWrite(stamp);
 *     }
 *   }
 *
 *   double distanceFromOrigin() { // һ����������
 *     long stamp = sl.tryOptimisticRead();
 *     double currentX = x, currentY = y;
 *     if (!sl.validate(stamp)) {
 *        stamp = sl.readLock();
 *        try {
 *          currentX = x;
 *          currentY = y;
 *        } finally {
 *           sl.unlockRead(stamp);
 *        }
 *     }
 *     return Math.sqrt(currentX * currentX + currentY * currentY);
 *   }
 *
 *   void moveIfAtOrigin(double newX, double newY) { // ����
 *     // Could instead start with optimistic, not read mode
 *     long stamp = sl.readLock();
 *     try {
 *       while (x == 0.0 && y == 0.0) {
 *         long ws = sl.tryConvertToWriteLock(stamp);
 *         if (ws != 0L) {
 *           stamp = ws;
 *           x = newX;
 *           y = newY;
 *           break;
 *         }
 *         else {
 *           sl.unlockRead(stamp);
 *           stamp = sl.writeLock();
 *         }
 *       }
 *     } finally {
 *       sl.unlock(stamp);
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.8
 * @author Doug Lea
 */
public class StampedLock implements java.io.Serializable {
    /*
     * Algorithmic notes:
     *
     * The design employs elements of Sequence locks
     * (as used in linux kernels; see Lameter's
     * http://www.lameter.com/gelato2005.pdf
     * and elsewhere; see
     * Boehm's http://www.hpl.hp.com/techreports/2012/HPL-2012-68.html)
     * and Ordered RW locks (see Shirako et al
     * http://dl.acm.org/citation.cfm?id=2312015)
     *
     * Conceptually, the primary state of the lock includes a sequence
     * number that is odd when write-locked and even otherwise.
     * However, this is offset by a reader count that is non-zero when
     * read-locked.  The read count is ignored when validating
     * "optimistic" seqlock-reader-style stamps.  Because we must use
     * a small finite number of bits (currently 7) for readers, a
     * supplementary reader overflow word is used when the number of
     * readers exceeds the count field. We do this by treating the max
     * reader count value (RBITS) as a spinlock protecting overflow
     * updates.
     *
     * Waiters use a modified form of CLH lock used in
     * AbstractQueuedSynchronizer (see its internal documentation for
     * a fuller account), where each node is tagged (field mode) as
     * either a reader or writer. Sets of waiting readers are grouped
     * (linked) under a common node (field cowait) so act as a single
     * node with respect to most CLH mechanics.  By virtue of the
     * queue structure, wait nodes need not actually carry sequence
     * numbers; we know each is greater than its predecessor.  This
     * simplifies the scheduling policy to a mainly-FIFO scheme that
     * incorporates elements of Phase-Fair locks (see Brandenburg &
     * Anderson, especially http://www.cs.unc.edu/~bbb/diss/).  In
     * particular, we use the phase-fair anti-barging rule: If an
     * incoming reader arrives while read lock is held but there is a
     * queued writer, this incoming reader is queued.  (This rule is
     * responsible for some of the complexity of method acquireRead,
     * but without it, the lock becomes highly unfair.) Method release
     * does not (and sometimes cannot) itself wake up cowaiters. This
     * is done by the primary thread, but helped by any other threads
     * with nothing better to do in methods acquireRead and
     * acquireWrite.
     *
     * These rules apply to threads actually queued. All tryLock forms
     * opportunistically try to acquire locks regardless of preference
     * rules, and so may "barge" their way in.  Randomized spinning is
     * used in the acquire methods to reduce (increasingly expensive)
     * context switching while also avoiding sustained memory
     * thrashing among many threads.  We limit spins to the head of
     * queue. A thread spin-waits up to SPINS times (where each
     * iteration decreases spin count with 50% probability) before
     * blocking. If, upon wakening it fails to obtain lock, and is
     * still (or becomes) the first waiting thread (which indicates
     * that some other thread barged and obtained lock), it escalates
     * spins (up to MAX_HEAD_SPINS) to reduce the likelihood of
     * continually losing to barging threads.
     *
     * Nearly all of these mechanics are carried out in methods
     * acquireWrite and acquireRead, that, as typical of such code,
     * sprawl out because actions and retries rely on consistent sets
     * of locally cached reads.
     *
     * As noted in Boehm's paper (above), sequence validation (mainly
     * method validate()) requires stricter ordering rules than apply
     * to normal volatile reads (of "state").  To force orderings of
     * reads before a validation and the validation itself in those
     * cases where this is not already forced, we use
     * Unsafe.loadFence.
     *
     * The memory layout keeps lock state and queue pointers together
     * (normally on the same cache line). This usually works well for
     * read-mostly loads. In most other cases, the natural tendency of
     * adaptive-spin CLH locks to reduce memory contention lessens
     * motivation to further spread out contended locations, but might
     * be subject to future improvements.
     */

    private static final long serialVersionUID = -6001602636862214147L;

    /** CPU���� */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** �߳̽������ǰ����������� */
    private static final int SPINS = (NCPU > 1) ? 1 << 6 : 0;

    /** ����ͷ�������ǰ������������� */
    private static final int HEAD_SPINS = (NCPU > 1) ? 1 << 10 : 0;

    /** ��������ǰ������������� */
    private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 16 : 0;

    /** �ȴ����������ʱ��һ��ʱ�� */
    private static final int OVERFLOW_YIELD_RATE = 7; // must be power 2 - 1

    /** �����֮ǰ���ڶ�ȡ��������λ����Ŀ */
    private static final int LG_READERS = 7;

    // Values for lock state and stamp operations
    private static final long RUNIT = 1L;
    private static final long WBIT  = 1L << LG_READERS; // 1000,0000
    private static final long RBITS = WBIT - 1L; // 111,1111
    private static final long RFULL = RBITS - 1L; // 111,1110
    private static final long ABITS = RBITS | WBIT; // 1111,1111
    private static final long SBITS = ~RBITS; // note overlap with ABITS

    // ��״̬��ʼ��ֵ; ����ʧ��ֵ0
    private static final long ORIGIN = WBIT << 1;

    // ȡ����ȡ��������ֵ so caller can throw IE
    private static final long INTERRUPTED = 1L;

    // �ڵ�״ֵ̬; order matters
    private static final int WAITING   = -1; // �ȴ�
    private static final int CANCELLED =  1; // ȡ��

    // �ڵ�ģʽ (int����boolean����)
    private static final int RMODE = 0;
    private static final int WMODE = 1;

    /** �ȴ��ڵ�  */
    static final class WNode { // д�ڵ�
        volatile WNode prev; // ǰ�ڵ�
        volatile WNode next; // ��һ���ڵ�
        volatile WNode cowait;    // ����
        volatile Thread thread;   // ����������null�߳�
        volatile int status;      // 0, WAITING, or CANCELLED
        final int mode;           // RMODE or WMODE
        WNode(int m, WNode p) { mode = m; prev = p; }
    }

    /** ͷ of CLH queue */
    private transient volatile WNode whead;
    /** β (last) of CLH queue */
    private transient volatile WNode wtail;

    // views
    transient ReadLockView readLockView;
    transient WriteLockView writeLockView;
    transient ReadWriteLockView readWriteLockView;

    /** ��״̬ */
    private transient volatile long state;
    /** ����Ķ����� ��״̬����������ʱ */
    private transient int readerOverflow;

    /**
     * ����һ���µ���, ��ʼ��Ϊ����״̬
     */
    public StampedLock() {
        state = ORIGIN; // 1,0000,0000
    }

    /**
     * ��ռ��ȡ������Ҫʱ������ֱ�����á�
     *
     * @return �����ڽ�����ת��ģʽ�Ĵ���
     */
    public long writeLock() {
        long s, next;  //  ������ȫ������������ƹ�acquireWrite ��s ��ǰ״ֵ̬��next ��һ��״ֵ̬
        return ((((s = state) & ABITS) == 0L && // 1,0000,0000 & 1111,1111 = 0 û�ж�����д��
                 U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ? // 1,1000,0000 ����д��
                next : acquireWrite(false, 0L)); // �ɹ�����next��ʧ�ܳ��Ի�ȡд��
    }

    /**
     * ��������������õģ����ռ�ػ�ȡ����
     *
     * @return �����ڽ�����ת��ģʽ�Ĵ��ǣ��������������Ϊ��
     */
    public long tryWriteLock() {
        long s, next;
        return ((((s = state) & ABITS) == 0L &&
                 U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
                next : 0L); // ��ȡ�ɹ�����state��ʧ�ܷ���0��
    }

    /**
     * ������ڸ���ʱ���ڿ��ò��ҵ�ǰ�߳�û�б��жϣ����ռ�Եػ�ȡ����
     * ��ʱ���ж��µ���Ϊ�뷽��Lock.tryLock(long,TimeUnit)ָ������Ϊƥ�䡣
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long tryWriteLock(long time, TimeUnit unit)
        throws InterruptedException {
        // ��ʱ��ת��Ϊ����
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) { // �ж��߳��Ƿ��ж�
            long next, deadline;
            // �ȵ�����ͼ��ȡд�����ɹ��򷵻�
            if ((next = tryWriteLock()) != 0L)
                return next;
            if (nanos <= 0L) // ��ʱ��ʧ��
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            if ((next = acquireWrite(true, deadline)) != INTERRUPTED) // ����ʱ�Ļ������ɹ�
                return next;
        }
        throw new InterruptedException();
    }

    /**
     * ��ռ��ȡ������Ҫʱ������ֱ�����û�ǰ�߳��жϡ�
     * �ж��µ���Ϊ��ΪLock.lockInterruptibly()����ָ������Ϊ��ƥ�䡣
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long writeLockInterruptibly() throws InterruptedException {
        long next;
        // �ж����׳��쳣
        if (!Thread.interrupted() &&
            (next = acquireWrite(true, 0L)) != INTERRUPTED)
            return next;
        throw new InterruptedException();
    }

    /**
     * �Ƕ�ռ�Ի�ȡ������Ҫʱ������ֱ������Ϊֹ��
     *
     * @return a stamp that can be used to unlock or convert mode
     */
    public long readLock() { // ��ȡ����
        long s = state, next;  // bypass acquireRead on common uncontended case
        // ͷβ�ڵ���� & ����û����� & ���ö����ɹ� = ��ȡ�ɹ� �������Ի�ȡ
        return ((whead == wtail && (s & ABITS) < RFULL &&
                 U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) ?
                next : acquireRead(false, 0L));
    }

    /**
     * ��������������õģ���Ƕ�ռ�Ի�ȡ����
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    public long tryReadLock() { // ��������ȡ
        for (;;) {
            long s, m, next;
            if ((m = (s = state) & ABITS) == WBIT) // д��״̬����ȡʧ��
                return 0L;
            else if (m < RFULL) { // û�����
                if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                    return next; // ��ȡ�ɹ�
            }
            else if ((next = tryIncReaderOverflow(s)) != 0L) // �������
                return next;
        }
    }

    /**
     * ������ڸ�����ʱ���ڿ��ò��ҵ�ǰ�߳�û�б��жϣ���Ƕ�ռ�Ի�ȡ����
     * ��ʱ���ж��µ���Ϊ�뷽��Lock.tryLock(long,TimeUnit)ָ������Ϊƥ�䡣
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long tryReadLock(long time, TimeUnit unit) // ��ʱ���ƣ�֧���ж�
        throws InterruptedException {
        long s, m, next, deadline;
        // ת������
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) { // ���ж�״̬
            // ��Ϊд��״̬
            if ((m = (s = state) & ABITS) != WBIT) {
                if (m < RFULL) { // ����δ���
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next; // �ɹ�����
                } // ������ȥ����
                else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            }
            if (nanos <= 0L) // С��0��ʧ��
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            // ���Ի�ȡ ���ж�
            if ((next = acquireRead(true, deadline)) != INTERRUPTED)
                return next;
        }
        throw new InterruptedException(); // �ж��쳣
    }

    /**
     * �Ƕ�ռ�Ի�ȡ������Ҫʱ������ֱ�����û�ǰ�߳��жϡ�
     * �ж��µ���Ϊ��ΪLock.lockInterruptibly()����ָ������Ϊ��ƥ�䡣
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long readLockInterruptibly() throws InterruptedException {
        long next;
        if (!Thread.interrupted() &&
            (next = acquireRead(true, 0L)) != INTERRUPTED) // ���жϳ��Ի�ȡ
            return next;
        throw new InterruptedException(); // �ж�
    }

    /**
     * ����һ���Ժ������֤�Ĵ��ǣ������ռ�����򷵻��㡣
     *
     * @return a stamp, or zero if exclusively locked
     */
    public long tryOptimisticRead() {
        long s;
        // ������дģʽ���ش��ǣ�����ʧ��
        return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
    }

    /**
     * ����Է��������Ĵ�����������û�б���ռ���򷵻�true��
     * �������Ϊ�㣬��ʼ�շ���false��������Ǳ�ʾ��ǰ���е�������ʼ�շ���true��
     * ʹ��δ��tryOptimisticRead()��ȡ��ֵ������������������ô˷���û�ж���Ч��������
     *
     * @param stamp a stamp
     * @return {@code true} if the lock has not been exclusively acquired
     * since issuance of the given stamp; else false
     */
    public boolean validate(long stamp) {
        // �����ڴ�����
        U.loadFence();
        // cas
        return (stamp & SBITS) == (state & SBITS);
    }

    /**
     * �����״̬������Ĵ���ƥ�䣬���ͷŻ�������
     *
     * @param stamp a stamp returned by a write-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlockWrite(long stamp) {
        WNode h;
        // �����ǰstate�ʹ���Ĵ��ǲ�ƥ�䣬���쳣
        if (state != stamp || (stamp & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        // �ͷ�����Ҫ�ڸ�λ���Ӽ�¼
        state = (stamp += WBIT) == 0L ? ORIGIN : stamp;
        // ͷ�ڵ㲻Ϊ�գ�����ͷ���״̬��Ϊ0
        if ((h = whead) != null && h.status != 0)
            // �ͷ��¸��ڵ�
            release(h);
    }

    /**
     * �����״̬������Ĵ���ƥ�䣬���ͷŷ���������
     *
     * @param stamp a stamp returned by a read-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlockRead(long stamp) {
        long s, m; WNode h;
        for (;;) { // ����
            // д״̬�ı� �� ���� �� д�� -�� ���쳣
            if (((s = state) & SBITS) != (stamp & SBITS) ||
                (stamp & ABITS) == 0L || (m = s & ABITS) == 0L || m == WBIT)
                throw new IllegalMonitorStateException();
            if (m < RFULL) { // ����δ���
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h); // �ͷŶ���
                    break;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L) // ����������
                break;
        }
    }

    /**
     * �����״̬������Ĵ���ƥ�䣬���ͷ�������Ӧģʽ��
     *
     * @param stamp a stamp returned by a lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlock(long stamp) { // ��д���������ͷ�
        long a = stamp & ABITS, m, s; WNode h;
        // �жϸ�λд����¼
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            // �����ǰ��������״̬���ֹ۶�״̬��ֱ���˳�
            if ((m = s & ABITS) == 0L)
                break;
            else if (m == WBIT) { // дģʽ
                // �����stam����дģʽ���˳�
                if (a != m)
                    break;
                // �ͷ�д������Ӽ�¼
                state = (s += WBIT) == 0L ? ORIGIN : s;
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return;
            }
            // �������ֹ۶����˳�
            else if (a == 0L || a >= WBIT)
                break;
                // ����û�����
            else if (m < RFULL) {
                // �ͷŶ���,�ɹ��˳���ʧ������
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return;
        }
        // ��¼���ԣ����쳣
        throw new IllegalMonitorStateException();
    }

    /**
     * �����״̬������Ĵ���ƥ�䣬��ִ�����²���֮һ��
     * ������Ǳ�ʾ����д�����򷵻��������ߣ�����ж��������д�����ã����ͷŶ���������д����
     * ���ߣ�������ֹ۶����������������ʱ�ŷ���д���ǡ��˷�����������������¶������㡣
     *
     * @param stamp a stamp
     * @return a valid write stamp, or zero on failure
     */
    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS, m, s, next;
        // ������¼��ͬ
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L) { // �������ֹ۶�
                if (a != 0L) // ������д��
                    break;
                if (U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
                    return next; // �����ɹ�
            }
            else if (m == WBIT) { // ��д��״̬
                if (a != m) // ��д�� ʧ��
                    break;
                return stamp; // ����д��״̬stamp
            }
            else if (m == RUNIT && a != 0L) { // ��ֻ��һ����������ֱ������Ϊд��
                if (U.compareAndSwapLong(this, STATE, s,
                                         next = s - RUNIT + WBIT))
                    return next;
            }
            else // �����˳�
                break;
        }
        return 0L;
    }

    /**
     * �����״̬������Ĵ���ƥ�䣬��ִ�����²���֮һ��
     * ������Ǳ�ʾ����д�������ͷ�������ö�����
     * ���ߣ�����Ƕ������򷵻��������ߣ����һ���ֹ۶����������һ��������
     * ����ֻ�����������õ�����²ŷ���һ���������˷�����������������¶������㡣
     *
     * @param stamp a stamp
     * @return a valid read stamp, or zero on failure
     */
    public long tryConvertToReadLock(long stamp) {
        long a = stamp & ABITS, m, s, next; WNode h;
        // ����¼
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            // ����
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                else if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next; // ֱ������
                }
                else if ((next = tryIncReaderOverflow(s)) != 0L) // �������
                    return next;
            }
            else if (m == WBIT) { // ����
                if (a != m)
                    break;
                state = next = s + (WBIT + RUNIT); // ��¼д������Ӷ���
                if ((h = whead) != null && h.status != 0)
                    release(h); // �ͷź����ڵ�
                return next;
            }
            else if (a != 0L && a < WBIT) // ���Ƕ�����ֱ�ӷ���
                return stamp;
            else
                break;
        }
        return 0L;
    }

    /**
     * �����״̬������Ĵ���ƥ�䣬��������Ǳ�ʾ�����������ͷ���������һ���۲���ǡ�
     * ���ߣ�������ֹ۶�ȡ��������֤�󷵻ء��÷�����������������¶�����0��
     * ��˿�����Ϊ��tryUnlock����һ����ʽʹ�á�
     *
     * @param stamp a stamp
     * @return a valid optimistic read stamp, or zero on failure
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long a = stamp & ABITS, m, s, next; WNode h;
        U.loadFence();
        for (;;) {
            // ����¼���
            if (((s = state) & SBITS) != (stamp & SBITS))
                break;
            if ((m = s & ABITS) == 0L) { // ����
                if (a != 0L)
                    break;
                return s; // ����
            }
            else if (m == WBIT) { // д��
                if (a != m)
                    break;
                state = next = (s += WBIT) == 0L ? ORIGIN : s; // ��¼д��¼�������Ļ�����ORIGIN����������֮ǰֵ
                // ͷ�ڵ㲻Ϊ�գ�����ͷ����״̬��Ϊ0
                if ((h = whead) != null && h.status != 0)
                    release(h); // �ͷ�
                return next;
            }
            else if (a == 0L || a >= WBIT) // �����˳�
                break;
            else if (m < RFULL) { // ����δ���
                if (U.compareAndSwapLong(this, STATE, s, next = s - RUNIT)) { // ����-1
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h); // ������ȫ�����������ͷ��¸��ڵ㡣
                    return next & SBITS;
                }
            }
            else if ((next = tryDecReaderOverflow(s)) != 0L) // �������
                return next & SBITS;
        }
        return 0L;
    }

    /**
     * �������д�������ͷ�����������Ҫ����ֵ���˷������ڴ����Ļָ����ܺ����á�
     *
     * @return {@code true} if the lock was held, else false
     */
    public boolean tryUnlockWrite() { // ���贫��stamp�ͷ�д��
        long s; WNode h;
        // �����ǰ����д״̬�����ͷ�ʧ��
        if (((s = state) & WBIT) != 0L) {
            // �ͷţ���λ��Ӽ�¼
            state = (s += WBIT) == 0L ? ORIGIN : s;
            if ((h = whead) != null && h.status != 0)
                release(h);
            return true;
        }
        return false;
    }

    /**
     * ������ж��������ͷŸö�����һ�γ��У�������Ҫ����ֵ���˷������ڴ����Ļָ����ܺ����á�
     *
     * @return {@code true} if the read lock was held, else false
     */
    public boolean tryUnlockRead() {
        long s, m; WNode h;
        while ((m = (s = state) & ABITS) != 0L && m < WBIT) {
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return true;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return true;
        }
        return false;
    }

    // status monitoring methods

    /**
     * ���ظ���״̬�����״̬�������������������
     */
    private int getReadLockCount(long s) {
        long readers;
        if ((readers = s & RBITS) >= RFULL)
            readers = RFULL + readerOverflow;
        return (int) readers;
    }

    /**
     * �������ǰ����ռ���򷵻�true��
     *
     * @return {@code true} if the lock is currently held exclusively
     */
    public boolean isWriteLocked() {
        return (state & WBIT) != 0L;
    }

    /**
     * �������ǰ�Ƕ�ռ�س��У��򷵻�true��
     *
     * @return {@code true} if the lock is currently held non-exclusively
     */
    public boolean isReadLocked() {
        return (state & RBITS) != 0L;
    }

    /**
     * ��ѯΪ�������еĶ������������÷������ڼ��ϵͳ״̬��������ͬ�����ơ�
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return getReadLockCount(state);
    }

    /**
     * ���ر�ʶ�������ַ���������״̬��
     * �����е�״̬�����ַ��������������ַ�����д�������ַ�����������:�����ǰ���еĶ���������
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        long s = state;
        return super.toString() +
            ((s & ABITS) == 0L ? "[Unlocked]" :
             (s & WBIT) != 0L ? "[Write-locked]" :
             "[Read-locks:" + getReadLockCount(s) + "]");
    }

    // views

    /**
     * ���ش�StampedLock����ͨ{@link Lock}��ͼ������{@link Lock# Lock}����ӳ�䵽
     * {@link #readLock}����������Ҳ���ơ�
     * ���ص�����֧��{@link����};����{@link Lock#newCondition()}�׳�
     * {@code UnsupportedOperationException}��
     *
     * @return the lock
     */
    public Lock asReadLock() {
        ReadLockView v;
        return ((v = readLockView) != null ? v :
                (readLockView = new ReadLockView()));
    }

    /**
     * ���ش�StampedLock����ͨ{@link Lock}��ͼ������{@link Lock# Lock}����ӳ�䵽
     * {@link #writeLock}����������Ҳ���ơ�
     * ���ص�����֧��{@link����};����{@link Lock#newCondition()}�׳�
     * {@code UnsupportedOperationException}��
     *
     * @return the lock
     */
    public Lock asWriteLock() {
        WriteLockView v;
        return ((v = writeLockView) != null ? v :
                (writeLockView = new WriteLockView()));
    }

    /**
     * ���ش�StampedLock��{@link ReadWriteLock}��ͼ������
     * {@link ReadWriteLock#readLock()}����ӳ�䵽{@link #asReadLock()}��
     * {@link ReadWriteLock#writeLock()}ӳ�䵽{@link #asWriteLock()}��
     *
     * @return the lock
     */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        return ((v = readWriteLockView) != null ? v :
                (readWriteLockView = new ReadWriteLockView()));
    }

    // view classes

    // ������ͼ
    final class ReadLockView implements Lock {
        public void lock() { readLock(); }
        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }
        public boolean tryLock() { return tryReadLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockRead(); }
        public Condition newCondition() { // ��֧��
            throw new UnsupportedOperationException();
        }
    }
    // д����ͼ
    final class WriteLockView implements Lock {
        public void lock() { writeLock(); }
        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }
        public boolean tryLock() { return tryWriteLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockWrite(); }
        public Condition newCondition() { // ��֧��
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock { // ʵ�ֶ�д��
        public Lock readLock() { return asReadLock(); }
        public Lock writeLock() { return asWriteLock(); }
    }

    // Unlock methods without stamp argument checks for view classes.
    // Needed because view-class lock methods throw away stamps.

    final void unstampedUnlockWrite() {
        WNode h; long s;
        if (((s = state) & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        state = (s += WBIT) == 0L ? ORIGIN : s;
        if ((h = whead) != null && h.status != 0)
            release(h);
    }

    final void unstampedUnlockRead() {
        for (;;) {
            long s, m; WNode h;
            if ((m = (s = state) & ABITS) == 0L || m >= WBIT)
                throw new IllegalMonitorStateException();
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    break;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                break;
        }
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        state = ORIGIN; // reset to unlocked state
    }

    // internals

    /**
     * ����ͨ�����Ƚ�״̬����λֵ����ΪRBITS������readerOverflow����ʾ������������Ȼ����£�Ȼ���ͷš�
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryIncReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        // ����������
        if ((s & ABITS) == RFULL) {
            // ��������127
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                // ���+1
                ++readerOverflow;
                // stateΪԭֵ
                state = s;
                return s; // ����ԭֵ
            }
        } // ����������²����ˣ�������ò�
        else if ((LockSupport.nextSecondarySeed() &
                  OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        return 0L;
    }

    /**
     * ���Եݼ�readerOverflow��
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryDecReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        // �����ǰ��ģʽ������
        if ((s & ABITS) == RFULL) {
            // �Ƚ�state����127
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                int r; long next;
                // �����ǰ��ǰ��¼���������0
                if ((r = readerOverflow) > 0) {
                    // �������1
                    readerOverflow = r - 1;
                    // ԭ��state
                    next = s;
                }
                else
                    // ���������¼-1
                    next = s - RUNIT;
                 state = next;
                 return next;
            }
        } // ��������ͷŶ���������ò�
        else if ((LockSupport.nextSecondarySeed() &
                  OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        return 0L;
    }

    /**
     * ����h�ļ̳���(ͨ����whead)��
     * ��ͨ����h�����ǣ������һ��ָ���ͺ��������Ҫ��wtail������
     * ��һ�������̱߳�ȡ��ʱ��������޷����ѻ�ȡ�̣߳�����ȡ�����������ṩ�˶���ı�����ʩ��ȷ�����ԡ�
     */
    private void release(WNode h) {
        if (h != null) { // ͷ�ڵ㲻Ϊ��
            WNode q; Thread w;
            // ���ͷ����״̬Ϊ�ȴ�״̬������״̬����Ϊ0
            U.compareAndSwapInt(h, WSTATUS, WAITING, 0);
            // ��β��㿪ʼ����ͷ�ڵ�Ϊֹ��Ѱ��״̬Ϊ�ȴ�״̬����Ϊ0����Ч�ڵ�
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            // ���Ѱ�ҵ���Ч�ڵ㲻Ϊ�գ��������Ӧ���߳�Ҳ��Ϊ�գ��������߳�
            if (q != null && (w = q.thread) != null)
                U.unpark(w);
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible t�������ж� and if so
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return ��һ��״̬, or �ж�
     */
    private long acquireWrite(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) { // ���ʱ����
            long m, s, ns;
            if ((m = (s = state) & ABITS) == 0L) { // ����ǰǡ��������������д���󷵻�
                if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT))
                    return ns;
            }
            else if (spins < 0) // �����ǰ����ռ����
                // ������û�����Ļ�����ȡ��������
                spins = (m == WBIT && wtail == whead) ? SPINS : 0; // д������ǰ�޶��У���������255������0
            else if (spins > 0) { // ����
                if (LockSupport.nextSecondarySeed() >= 0)// ��֪����ɶ���ã���Զ��������
                    --spins; // ���Դ�����һ
            }
            // ���Խ�������ʼ�������
            else if ((p = wtail) == null) { // ��ʼ������
                WNode hd = new WNode(WMODE, null); // ��ǰ�εĽڵ�
                if (U.compareAndSwapObject(this, WHEAD, null, hd)) // �滻����ͷ���
                    wtail = hd; // β�ڵ�=ͷ���
            }
            else if (node == null)
                node = new WNode(WMODE, p); // ͷ�ڵ����½ڵ�
            else if (node.prev != p) // β�ڵ�ı䣬��������
                node.prev = p;
            else if (U.compareAndSwapObject(this, WTAIL, p, node)) { // ����β�ڵ�
                p.next = node;
                break; // ������гɹ�������ѭ��
            }
        }
// ѭ��������ǰ�߳�
        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            if ((h = whead) == p) { // ��ͷ������β�ڵ�
                if (spins < 0)
                    spins = HEAD_SPINS; // ����������ֵ
                else if (spins < MAX_HEAD_SPINS) // �������С���������ֵ������������
                    spins <<= 1;
                for (int k = spins;;) { // ��ͷ������
                    long s, ns;
                    if (((s = state) & ABITS) == 0L) { // �����������ȡ��
                        if (U.compareAndSwapLong(this, STATE, s,
                                                 ns = s + WBIT)) {
                            whead = node; // ͷ�ڵ�����Ϊ��ǰ�ڵ�
                            node.prev = null; // ����gc
                            return ns; // ���ص�ǰstate
                        }
                    }
                    else if (LockSupport.nextSecondarySeed() >= 0 &&
                             --k <= 0)
                        break; // ������ʱ������ѭ��
                }
            }
            else if (h != null) { // ��ͷ�ڵ㲻Ϊ�գ��ͷŵȴ���
                WNode c; Thread w;
                // ���ͷ����cowait���в�Ϊ�գ�����cowait����
                while ((c = h.cowait) != null) { // ѭ�����ѵȴ��߳�
                    if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null)
                        U.unpark(w); // �ͷŶ��߳�
                }
            }
            if (whead == h) { // ���ͷ��㲻��
                // �����ǰ�ڵ��ǰ�ڵ㲻��β���
                if ((np = node.prev) != p) {
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                // �����ǰ�ڵ��ǰ�ڵ�״̬Ϊ0������ǰ���ڵ�����Ϊ�ȴ�״̬
                else if ((ps = p.status) == 0)
                    U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
                    // �����ǰ�ڵ�ǰ�ڵ�״̬Ϊȡ��
                else if (ps == CANCELLED) {
                    // �������õ�ǰ�ڵ��ǰ���ڵ�
                    if ((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time; // �������ʱ��Ϊ0������ֱ������
                    if (deadline == 0L)
                        time = 0L;
                        // ���ʱ���ѳ�ʱ��ȡ����ǰ�ڵ�
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    // ��ȡ��ǰ�߳�
                    Thread wt = Thread.currentThread();
                    // �����߳�Thread��parkblocker���ԣ���ʾ��ǰ�̱߳�˭���������ڼ���߳�ʹ��
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if (p.status < 0 && (p != h || (state & ABITS) != 0L) &&
                        whead == h && node.prev == p)
                        // ������ǰ�߳�
                        U.park(false, time);  // ���� LockSupport.park
                    // ��ǰ�ڵ��߳�Ϊ��
                    node.thread = null;
                    U.putObject(wt, PARKBLOCKER, null);
                    // �������Ĳ���interruptibleΪtrue�����ҵ�ǰ�߳��жϣ�ȡ����ǰ�ڵ�
                    if (interruptible && Thread.interrupted())
                        // ȡ���ڵ�
                        return cancelWaiter(node, node, true);
                }
            }
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible true �������ж�Ϊtrue
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return next state, or INTERRUPTED
     */
    private long acquireRead(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) {
            WNode h;
            // ���ͷ������β��㣬������
            if ((h = whead) == (p = wtail)) {
                for (long m, s, ns;;) {
                    // �������δ��������ö�+1�ɹ� ���򷵻ػ�ȡ�ɹ�
                    if ((m = (s = state) & ABITS) < RFULL ?
                        U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) :
                        (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L))
                        return ns;
                    else if (m >= WBIT) { // ����-1
                        if (spins > 0) {
                            if (LockSupport.nextSecondarySeed() >= 0)
                                --spins;
                        }
                        else {
                            if (spins == 0) { // ��������0
                                WNode nh = whead, np = wtail;
                                // ���ͷβ�ڵ�δ�ı䣬��ͷβ�ڵ㲻��ȣ��˳�����
                                if ((nh == h && np == p) || (h = nh) != (p = np))
                                    break;
                            }
                            spins = SPINS; // ������ʼֵ
                        }
                    }
                }
            }
            if (p == null) { // �������Ϊnull����ʼ������
                WNode hd = new WNode(WMODE, null);
                if (U.compareAndSwapObject(this, WHEAD, null, hd)) // ����ͷ�ڵ�
                    wtail = hd;
            }
            else if (node == null) // ����ǰ�ڵ�Ϊ�գ����쵱ǰ�ڵ�
                node = new WNode(RMODE, p);
                // ��ͷβ�ڵ���ȣ���ǰ��״̬��Ϊ����״̬
            else if (h == p || p.mode != RMODE) {
                if (node.prev != p) // ���õ�ǰ�ڵ�
                    node.prev = p;
                else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                    p.next = node;
                    break; // ��ӳɹ�������
                }
            } // ����ǰ�ڵ����β����cowait�����У����ʧ�ܣ��򽫵�ǰ�ڵ��cowait��Ϊnull
            else if (!U.compareAndSwapObject(p, WCOWAIT,
                                             node.cowait = p.cowait, node))
                node.cowait = null;
            else { // �����ǰ���в�Ϊ�գ���ǰ�ڵ㲻Ϊ�գ����ҵ�ǰΪ����״̬�����Ҽ���β����ʧ��
                for (;;) {
                    WNode pp, c; Thread w;
                    // ���ͷ�ڵ��cowait���в�Ϊ�գ��������߳�Ҳ��Ϊnull������cowait����
                    if ((h = whead) != null && (c = h.cowait) != null &&
                        U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null) // help release
                        U.unpark(w); // ����cowͷait�����еĽڵ��߳�
                    // �����ǰͷ�ڵ�Ϊβ����ǰ���ڵ㣬����ͷβ�ڵ���ȣ�����β����ǰ�ڵ�Ϊ��
                    if (h == (pp = p.prev) || h == p || pp == null) {
                        long m, s, ns;
                        do { // �жϵ�ǰ�Ƿ����ڶ�״̬����û�������������1��
                            if ((m = (s = state) & ABITS) < RFULL ?
                                U.compareAndSwapLong(this, STATE, s,
                                                     ns = s + RUNIT) :
                                (m < WBIT &&
                                 (ns = tryIncReaderOverflow(s)) != 0L)) // ��������������
                                return ns;
                        } while (m < WBIT); // ��ǰstate����дģʽ�����ܽ�������
                    }
                    if (whead == h && p.prev == pp) { // ���ͷ�ڵ�û�иı䣬����β����ǰ���ڵ㲻��
                        long time;
                        // ���β����ǰ���ڵ�Ϊ�գ���ͷβ�ڵ���ȣ���β����״̬Ϊȡ��
                        if (pp == null || h == p || p.status > 0) {
                            // ����ǰ�ڵ�����Ϊ�գ��˳�ѭ��
                            node = null; // throw away
                            break;
                        }
                        // �������ĳ�ʱʱ���Ѿ����ڣ�����ǰ�ڵ�ȡ��
                        if (deadline == 0L)
                            time = 0L;
                        else if ((time = deadline - System.nanoTime()) <= 0L)
                            return cancelWaiter(node, p, false);
                        // ��ȡ��ǰ�̣߳�������������
                        Thread wt = Thread.currentThread();
                        U.putObject(wt, PARKBLOCKER, this);
                        node.thread = wt;
                        if ((h != pp || (state & ABITS) == WBIT) &&
                            whead == h && p.prev == pp)
                            // ������ǰ�ڵ�
                            U.park(false, time);
                        node.thread = null;
                        // ������������������Ϊnull
                        U.putObject(wt, PARKBLOCKER, null);
                        // �жϵĻ���ȡ���ȴ�
                        if (interruptible && Thread.interrupted())
                            return cancelWaiter(node, p, true);
                    }
                }
            }
        }

        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            // ���ͷβ�ڵ����
            if ((h = whead) == p) {
                if (spins < 0)
                    spins = HEAD_SPINS; // ������ʼֵ
                else if (spins < MAX_HEAD_SPINS) // spinС������������������
                    spins <<= 1;
                for (int k = spins;;) { // spin at head
                    long m, s, ns;
                    if ((m = (s = state) & ABITS) < RFULL ? // ������С�����ֵ
                        U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) : // ��ȡ�ɹ�
                        (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) { // �������ֵ
                        WNode c; Thread w;
                        whead = node;
                        node.prev = null;
                        while ((c = node.cowait) != null) {
                            if (U.compareAndSwapObject(node, WCOWAIT,
                                                       c, c.cowait) &&
                                (w = c.thread) != null)
                                // �ͷŵ�ǰ�ڵ�
                                U.unpark(w);
                        }
                        return ns; // ���سɹ�
                    }
                    else if (m >= WBIT && // ����ǰΪд״̬������
                             LockSupport.nextSecondarySeed() >= 0 && --k <= 0)
                        break;
                }
            }
            else if (h != null) { // ���ͷ��㲻Ϊ��
                WNode c; Thread w;
                while ((c = h.cowait) != null) { // ͷ���Ķ��в�Ϊ�գ�ѭ������cowait�����У��̲߳�Ϊ�յ��߳�
                    if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null)
                        U.unpark(w); // ����
                }
            }
            if (whead == h) { // ��ͷ���û�ı�
                if ((np = node.prev) != p) { // �����ǰ�ڵ��ǰ�ڵ㲻����β���
                    // ǰ�ڵ㲻Ϊ��
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                else if ((ps = p.status) == 0) // �����ǰ�ڵ��ǰ���ڵ�״̬Ϊ0��
                    U.compareAndSwapInt(p, WSTATUS, 0, WAITING); // ״̬�޸�Ϊ�ȴ�״̬
                else if (ps == CANCELLED) { // ����ȡ��״̬
                    if ((pp = p.prev) != null) { // ����
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time;
                    if (deadline == 0L) // ��ʱȡ��
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    Thread wt = Thread.currentThread();
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if (p.status < 0 &&
                        (p != h || (state & ABITS) == WBIT) &&
                        whead == h && node.prev == p)
                        U.park(false, time); // ����
                    node.thread = null; // ���Ѻ�����Ϊ��
                    U.putObject(wt, PARKBLOCKER, null); // ���Ѻ�����Ϊ��
                    if (interruptible && Thread.interrupted()) // �жϵĻ�
                        return cancelWaiter(node, node, true); // ȡ���ȴ��ڵ�
                }
            }
        }
    }

    /**
     * ����ڵ�ǿգ���ǿ��ȡ��״̬���ڿ��ܵ�����½���Ӷ����н�ƴ�ӳ�����
     * �������κ�cowaiter(�ڵ�����cowaiter�����������)�����κ�����£�
     * ������ǿ��еģ�������ͷŵ�ǰ��first waiter��(ʹ��null����������һ�������ͷ���ʽ��
     * Ŀǰ������Ҫ�������ڽ������ܵ�ȡ�������¿�����Ҫ)��
     * ����AbstractQueuedSynchronizer��ȡ��������һ�ֱ���(�μ�AQS�ڲ��ĵ��е���ϸ˵��)��
     *
     * @param node if nonnull, the waiter
     * @param group either node or the group node is cowaiting with
     * @param interrupted if already interrupted
     * @return INTERRUPTED if interrupted or Thread.interrupted, else zero
     */
    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        // node��groupΪͬһ�ڵ㣬Ҫȡ���Ľڵ㣬����Ϊnullʱ��
        if (node != null && group != null) {
            Thread w;
            node.status = CANCELLED; // ����ǰ�ڵ��״̬����Ϊȡ��״̬
            // unsplice cancelled nodes from group
            // �����ǰҪȡ���Ľڵ��cowait���в�Ϊ�գ�����cowait������ȡ���Ľڵ�ȥ��
            for (WNode p = group, q; (q = p.cowait) != null;) {
                if (q.status == CANCELLED) {
                    U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                    p = group; // restart
                }
                else
                    p = q;
            }
            // group��nodeΪͬһ�ڵ�
            if (group == node) {
                // ����״̬û��ȡ����cowait�����еĽڵ�
                for (WNode r = group.cowait; r != null; r = r.cowait) {
                    if ((w = r.thread) != null)
                        U.unpark(w);       // wake up uncancelled co-waiters
                }
                // ���䵱ǰȡ���ڵ��ǰ���ڵ����һ���ڵ�����Ϊ��ǰȡ���ڵ��next�ڵ�
                for (WNode pred = node.prev; pred != null; ) { // unsplice
                    WNode succ, pp;        // find valid successor
                    while ((succ = node.next) == null ||
                           succ.status == CANCELLED) {
                        WNode q = null;    // find successor the slow way
                        for (WNode t = wtail; t != null && t != node; t = t.prev)
                            if (t.status != CANCELLED)
                                q = t;     // don't link if succ cancelled
                        if (succ == q ||   // ensure accurate successor
                            U.compareAndSwapObject(node, WNEXT,
                                                   succ, succ = q)) {
                            if (succ == null && node == wtail)
                                U.compareAndSwapObject(this, WTAIL, node, pred);
                            break;
                        }
                    }
                    if (pred.next == node) // unsplice pred link
                        U.compareAndSwapObject(pred, WNEXT, node, succ);
                    if (succ != null && (w = succ.thread) != null) {
                        succ.thread = null;
                        U.unpark(w);       // wake up succ to observe new pred
                    }
                    if (pred.status != CANCELLED || (pp = pred.prev) == null)
                        break;
                    node.prev = pp;        // repeat if new pred wrong/cancelled
                    U.compareAndSwapObject(pp, WNEXT, pred, succ);
                    pred = pp;
                }
            }
        }
        WNode h; // Possibly release first waiter
        while ((h = whead) != null) {
            long s; WNode q; // similar to release() but check eligibility
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (h == whead) {
                if (q != null && h.status == 0 &&
                    ((s = state) & ABITS) != WBIT && // waiter is eligible
                    (s == 0L || q.mode == RMODE))
                    release(h);
                break;
            }
        }
        return (interrupted || Thread.interrupted()) ? INTERRUPTED : 0L;
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long STATE;
    private static final long WHEAD;
    private static final long WTAIL;
    private static final long WNEXT;
    private static final long WSTATUS;
    private static final long WCOWAIT;
    private static final long PARKBLOCKER;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = StampedLock.class;
            Class<?> wk = WNode.class;
            STATE = U.objectFieldOffset
                (k.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset
                (k.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset
                (k.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset
                (wk.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset
                (wk.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset
                (wk.getDeclaredField("cowait"));
            Class<?> tk = Thread.class;
            PARKBLOCKER = U.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
