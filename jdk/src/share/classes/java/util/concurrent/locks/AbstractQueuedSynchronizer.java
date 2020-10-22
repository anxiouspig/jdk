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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import sun.misc.Unsafe;

/**
 * �ṩһ�������ʵ�ֻ����Ƚ��ȳ�(FIFO)�ȴ����е������������ͬ����(�ź������¼���)��
 * ����౻��Ƴɴ�������͵�ͬ���������û�������Щͬ���������ڵ���ԭ��intֵ����ʾ״̬��
 * ������붨��ı����״̬���ܱ����ķ�������Щ�������������״̬���ڱ���ȡ���ͷŵĶ�����ζ��ʲô��
 * ���ǵ���Щ��������е���������ִ�����е��ŶӺ��������ơ�
 * �������ά������״̬�ֶΣ�����ֻ��ʹ��getState()��setState(int)��
 * compareAndSetState(int, int)�����Զ����µ�intֵ�Żᱻͬ�����١�
 *
 * <p>����Ӧ�ö���Ϊ�ǹ������ڲ������࣬����ʵ���������ͬ�����ԡ�
 * ��AbstractQueuedSynchronizer��ʵ���κ�ͬ���ӿڡ�
 * �෴������������acquireInterruptibly(int)�����ķ�����
 * ��Щ�������Ա������������ص�ͬ�����ʵ��ص�����ʵ�����ǵĹ���������
 *
 * <p>����֧��Ĭ�϶�ռģʽ�͹���ģʽ�е�һ�ֻ����֡�
 * ���Զ�ռģʽ��ȡʱ�������̳߳��ԵĻ�ȡ���޷��ɹ���
 * �ɶ���̻߳�ȡ�Ĺ���ģʽ����(����һ��)�ɹ�������ಢ������⡱��Щ���죬
 * �����ڻ�е�����ϣ���������ģʽ��ȡ�ɹ�ʱ��
 * ��һ���ȴ����߳�(������ڵĻ�)Ҳ����ȷ�����Ƿ�Ҳ���Ի�ȡ��
 * �ڲ�ͬģʽ�µȴ����̹߳�����ͬ��FIFO���С�
 * ͨ����ʵ������ֻ֧������һ��ģʽ����������ģʽ�����Է������ã�������ReadWriteLock�С�
 * ֻ֧�ֶ�ռ����ģʽ�����಻��Ҫ����֧��δʹ��ģʽ�ķ�����
 *
 * <p>����ඨ����һ��Ƕ�׵�AbstractQueuedSynchronizer.ConditionObject��,
 * ������������������ʵ��֧�ֶ�ռģʽ�ķ���isHeldExclusively()����ͬ���Ƿ�ֻ�Ե�ǰ�̳߳���,
 * release(int)���������뵱ǰgetState()ֵ��ȫ�ͷŸö���,��acquire(int),
 * ������������״ֵ̬,���ջ�õĸö���ָ�����ǰ��״̬��
 * AbstractQueuedSynchronizer�������ᴴ��������������������������������Լ����
 * �Ͳ�Ҫʹ����AbstractQueuedSynchronizer����Ϊ��
 * ��Ȼ����������ȡ������ͬ����ʵ�ֵ����塣
 *
 * <p>�����ṩ�ڲ����еļ�顢���ͼ��ӷ������Լ�������������Ʒ�����
 * ���Ը�����Ҫʹ��AbstractQueuedSynchronizer�����ǵ��������У���ʵ��ͬ�����ơ�
 *
 * <p>���������л�ֻ�洢�ײ��ԭ������ά��״̬����˷����л������п��̶߳��С�
 * ��Ҫ���л��ĵ������ཫ����һ��readObject�������ڷ����л�ʱ����ָ�����֪�ĳ�ʼ״̬��
 *
 * <h3>ʹ��</h3>
 *
 * <p>Ϊ��ʹ���������Ϊͬ���Ļ������Ķ����µķ�����
 * ����ͨ���鿴/�޸�ͬ��״̬ͨ��getState()��setState(int)��
 * and/or compareAndState(int, int)��
 *
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}
 * </ul>
 *
 * ��Щ������ÿһ�������׳�UnsupportedOperationException��
 * ʵ����Щ���������ڲ�ʵ���̰߳�ȫ��Ӧ�ô����ϼ��̷������ġ�
 * ������Щ������֧��ʹ����Щ�ࡣ������������final��Ϊ���ǲ��ܶ����仯��
 *
 * <p>�����Ҳ���ִ�aqs�̳еķ���ʹ�ñ�֤�߳�ӵ��һ����������
 * �����ʹ�����ǣ�����ܼ�����Ϲ���ȥ�����û������̳߳�������
 *
 * <p>��ʹ��������ڲ��Ƚ��ȳ�����Ϊ�����������Զ����Ƚ��ȳ�Э�顣
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>���û��ӣ�������߳�</em>;
 *        <em>����������ǰ�߳�</em>;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>���ѵ�һ�������߳�</em>;
 * </pre>
 *
 * (����ģʽ����ͬ�ĵ��ǿ��ܵ��ü����ź�)
 *
 * <p id="barging">��Ϊ����ȡ�����������ǰ��
 * һ���µĻ�ȡ�����߳̿������ڱ���ǰ����������ӡ�
 * Ȼ�������Ҫ������Զ���tryAcquire �� tryAcquireShared
 * ȥ����ͨ������һ�������۲췽��������ṩһ����ƽ���Ƚ��ȳ��Ļ��˳��
 * �ر�ģ�����Ĺ�ƽͬ�����Զ���ΪtryAcquireȥ����false���
 * hasQueuedPredecessor()��һ�������ر����ȥ���ڹ�ƽʵ�֣�����true��
 * ��������ʱ���ܵġ�
 *
 * <p>�������Ϳ�������ͨ����Ĭ�ϵ�barging
 * (Ҳ��Ϊ̰�Ĳ��ԡ��������Ժ�convoey -avoidance)����������ߵġ�
 * ��Ȼ���ܱ�֤���ǹ�ƽ�Ļ��޼����ģ����������Ķ����߳��ڽ���Ķ����߳�֮ǰ�����������ã�
 * ����ÿ���������ö�����ƫ�еĻ���ɹ��ضԿ������̡߳�
 * ���⣬��Ȼ��ȡ������ͨ����������������ת���������ǿ���ִ�ж��tryAcquire���ã�
 * ��������֮ǰ�����������㡣����ռ��ͬ��ֻ���ݳ���ʱ������ṩ����ת�Ĵ󲿷ֺô���
 * ������ռ��ͬ��������ʱ����û�д󲿷����Ρ�
 * �����Ҫ��������ͨ��ǰ��ĵ�������ȡ���С�����·�������ķ�������ǿ����������
 * ������ҪԤ�ȼ��hascontend()��/��hasQueuedThreads()���Ա����ͬ�������ܲ���������ʱ����������
 *
 * <p>������ṩһ����Ч�ĺͲ���ͬ���Ļ���ͨ��ָ��ͬ����ʹ�÷�Χ������������
 * state,acquire,release��������һ���ڲ����Ƚ��ȳ��Ķ��С����ⲻ����ʱ��
 * ����Խ���ͬ����һ����ˮƽʹ��actomic�࣬�������Ѷ��к�Lockpark����֧�֡�
 *
 * <h3>Usage Examples</h3>
 *
 * <p>����һ����������������࣬��0�������״̬��1ȥ��������״̬��
 * ��һ���������������ϸ��õ�ǰ�̣߳���������κη�ʽȥ�����ӡ�
 * ��Ҳ֧�������ͱ�¶����������һ����
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // �����ڲ��İ�����
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // ��ʾ�Ƿ�����״̬
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // ���״̬Ϊ0����Ի�ȡ��
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // ����  ��ʹ��
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // ����״̬Ϊ0���ͷ���
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // �ṩһ��Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // ǡ���ز���
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // ���½���״̬
 *     }
 *   }
 *
 *   // ͬ�������������м��ѵĹ���
 *   private final Sync sync = new Sync();
 *
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * <p>����һ��ռ������CountDownLatch��������Ҫ��һ�������źš�
 * ��Ϊһ��ռ�����Ƿ������ģ������������ȡ���ͷŷ�����
 *
 *  <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * ����һ����ʼͬ��״̬Ϊ0����{@code AbstractQueuedSynchronizer}ʵ����
     */
    protected AbstractQueuedSynchronizer() { }

    /**
     * �ȴ����нڵ��ࡣ
     *
     * <p>�ȴ������ǡ�CLH��(Craig��Landin��Hagersten)�����еı��塣
     * CLH��ͨ���������������෴������ʹ������������ͬ����������ʹ����ͬ�Ļ������ԣ�
     * ������ڵ��ǰ���б�������̵߳�һЩ������Ϣ��ÿ���ڵ��еġ�status���ֶθ����߳��Ƿ�Ӧ��������
     * һ���ڵ�������ǰһ���ڵ��ͷ�ʱ�����źš����е�ÿ���ڵ���Ϊһ���ض�֪ͨ��ʽ�ļ�������
     * ������һ���ȴ��̡߳�״̬�ֶβ������߳��Ƿ��������ȡ����һ���߳��Ƕ����еĵ�һ���̣߳�
     * �����᳢ܻ�Ի�ȡ�������ǳ�Ϊ��һ�����ܱ�֤�ɹ�;��ֻ�������Ǿ�����Ȩ����
     * ��ˣ���ǰ�����ľ������߳̿�����Ҫ���µȴ���
     *
     * <p>Ҫ���뵽CLH���У�����Ҫԭ���Եؽ�����Ϊ��β���ӽ�����
     * Ҫ�˳����У�ֻ������head�ֶΡ�
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>��nsertion����CLH����ֻ��Ҫ�ڡ�tail����ִ��һ��ԭ�Ӳ�����
     * ��˴�unqueued��queued��һ���򵥵�ԭ�ӷֽ�㡣
     * ���Ƶأ��˳�����ֻ�漰���¡�head�������ǣ��ڵ���Ҫ������Ĺ�����ȷ��˭�����ǵļ����ߣ�
     * ����ԭ����Ϊ�˴���ʱ���жϿ��ܵ��µ�ȡ����
     *
     * <p>��prev������(��ԭ����CLH����û��ʹ��)��Ҫ���ڴ���ȡ����
     * ���һ���ڵ㱻ȡ�������ĺ����(ͨ��)�ᱻ�������ӵ�һ��û��ȡ����ǰ���ߡ�
     *
     * <p>���ǻ�ʹ�á�next��������ʵ���������ơ�ÿ���ڵ���߳�id���������Լ��Ľڵ��У�
     * ���ǰһ���ڵ�ͨ��������һ��������֪ͨ��һ���ڵ㻽�ѣ���ȷ�������ĸ��̡߳�
     * ȷ������߱������ʹ���¼�����еĽڵ���������ǰ���ġ���һ�����ֶΡ�
     * ���ڵ�ĺ�̽ڵ�Ϊ��ʱ��ͨ����ԭ�Ӹ��µġ�β��������������������⡣
     * (���ߣ����仰˵����һ��������һ���Ż����������ͨ������Ҫ����ɨ�衣)
     *
     * <p>�����ڻ����㷨��������һЩ�����ԡ���Ϊ���Ǳ�����ѯ�Ƿ�ȡ�������ڵ㣬
     * �������ǿ��ܻ���Ա�ȡ���Ľڵ�����ǰ�滹���ں��档�����һ����ķ����ǣ�
     * ������ȡ����ȡ�������ߣ��������ȶ����µ�ǰ���ϣ����������ܹ�ȷ��һ��δȡ����ǰ�����е���һ���Ρ�
     *
     * <p>CLH������Ҫһ���鹹��ͷ�ڵ��������������ǲ�����ʩ��ʱ�������ǣ�
     * ��Ϊ�������û�����ã��ͻ��˷Ѿ������෴���ڵ�һ������ʱ����ڵ㲢����ͷ��βָ�롣
     *
     * <p>�ȴ��������߳�ʹ����ͬ�Ľڵ㣬��ʹ�ö�������ӡ�
     * ����ֻ��Ҫ�ڼ�(�ǲ���)���Ӷ��������ӽڵ㣬
     * ��Ϊ����ֻ�ڶ�ռʱ�����ʡ��ڵȴ�ʱ���ڵ㱻���뵽���������С�
     * �յ��źź󣬽ڵ㱻ת�Ƶ������С�״̬�ֶε�����ֵ���ڱ�ǽڵ����ڵĶ��С�
     *
     * <p>��лDave Dice��Mark Moir��Victor Luchangco��Bill Scherer��Michael Scott��
     * �Լ�JSR-166ר����ĳ�Ա�����Ƕ�����������ṩ�����õ��뷨�����ۺ�������
     */
    static final class Node {
        /** ����ģʽ�µȴ��Ľڵ� */
        static final Node SHARED = new Node();
        /** ����ģʽ�µȴ��Ľڵ� */
        static final Node EXCLUSIVE = null;

        /** ��ʾ�߳�ȡ�� */
        static final int CANCELLED =  1;
        /** �����߳���Ҫ���� */
        static final int SIGNAL    = -1;
        /** ����״̬���߳���Ҫ���� */
        static final int CONDITION = -2;
        /**
         * ��һ�λ�ȡ�������������ƴ���
         */
        static final int PROPAGATE = -3;

        /**
         * ״̬�ֶ�:
         *
         *   SIGNAL:     �˽ڵ�ĺ����ڵ㣨�򽫳�Ϊ�����ڵ㣩��������ͨ��park����
         *               ���Ե����ͷŻ���ȡ����ʱ�򣬵�ǰ�ڵ��������ڵ㡣Ϊ�˱��⾺����
         *               ��ȡ���������ж�������Ҫһ���ź�����Ȼ������ԭ���ԵĻ�ȡ��֮����ʧ������»�������
         *
         *   CANCELLED:  ���ڳ�ʱ���жϣ���ǰ�ڵ��ȡ�����ڵ㲻���뿪��״̬���ر�ģ�ȡ�����̲߳����ٴ�������
         *
         *   CONDITION:  �˽ڵ㵱ǰ���������С�����status����Ϊ0��ʱ�򣬷�����������ͬ�����С�
         *              �����ֵ�����ʹ�ò������ֶε�������;�������򻯽ṹ��
         *
         *   PROPAGATE:  һ���ͷŹ�����Ӧ�ô��ݸ������ڵ㡣��doReleaseShared��������ͷ��㣩����Ϊ��ȷ������������
         *              ��ʹ���������Ѿ��ж��ˡ�
         *
         * 0�����϶����ǡ�
         *
         * ��Щֵ������ʹ�á��Ǹ���ֵ��ζ��һ���ڵ㲻��Ҫ���������ԣ�������ڵ㲻��Ҫȥ����ر��ֵ������Ϊ�źš�
         *
         * ����ֶζ�������ͬ���ڵ��ʼ��Ϊ0�����������ڵ�Ϊ-2����cas�޸ģ�������ܣ�volatileд�룩
         */
        volatile int waitStatus;

        /**
         * ��ǰ �ڵ�/�߳� ������Ϊ�˼��״̬���ӵ�ǰ�εĽڵ㣬�������ʱָ��������ʱ��Ϊnull��Ϊ��GC����
         * ���ԣ�һ��������ȡ�������ҵ�һ����ȡ���Ľڵ�ʱ�����������Ѿ��뿪��ȡ���Ľڵ㡣
         * ��Ϊͷ��㲻��ȡ����һ���ڵ��Ϊͷ���ֻ�������»������ʱ��һ��ȡ�����̲߳���ɹ��������
         * һ���߳�ֻ��ȡ�����Լ�������ȡ�������ڵ㡣
         */
        volatile Node prev;

        /**
         * ��ǰ�ڵ���߳����ͷŵ�ʱ���ѵ����Ӽ��εĽڵ㡣
         * ���ʱָ��������ȡ����ǰ�Σ�������ʱ��Ϊnull��Ϊ��GC����
         * ��Ӳ���û��ָ��ǰ�ε���һ���ֶΣ����Կ�����һ���ֶ�Ϊnull����ζ�ŵ��˶���β��
         * ȡ���ڵ����һ���ֶ�����Ϊ���Լ�����ʧnull����������ͬ�����С�
         */
        volatile Node next;

        /**
         * �ڵ���ӵ��̡߳�����ʱ��ʼ����ʹ�ú���Ϊnull��
         */
        volatile Thread thread;

        /**
         * ����������ֵ�������ӵ���һ���ȴ��ڵ㣬��Ϊ�������н����뵱������������
         * ����ֻ��Ҫһ�򵥵����Ӷ���ȥ���нڵ㵱�����ȴ�ʱ��
         * ����֮���ת�ƶ���ȥ���»�ȡ����Ϊ�������������ģ����Ǳ���һ���ֶ�������ֵȥָ������ģ�͡�
         */
        Node nextWaiter;

        /**
         * ����ڵ��ڹ���ģʽ�µȴ����򷵻�true��
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * ����֮ǰ�Ľڵ㣬������null��ʱ���׳�NullPointterException��
         * ��ǰ�β�Ϊnullʱʹ�á����null����ʡ�ԣ����ǿ��԰���VM��
         *
         * @return �ڵ��ǰ��
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            // ���ǰһ���ڵ�Ϊnull�����쳣�����򷵻�
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // ���ڳ�ʼ��ͷ�͹���ı�־
        }

        Node(Thread thread, Node mode) {     // ������ӵȴ���
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Conditionʹ��
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * �ȴ����е�ͷ���������س�ʼ�������˳�ʼ��������ͨ������setHead�޸ġ�
     * ע�⣺���ͷ�����ˣ��ȴ�״̬����֤��ȡ����
     */
    private transient volatile Node head;

    /**
     * �ȴ����е�β�����ӳٳ�ʼ������ͨ������enq�޸�������µĵȴ��ڵ㡣
     */
    private transient volatile Node tail;

    /**
     * ͬ��״̬��
     */
    private volatile int state;

    /**
     * ����ͬ��״̬�ĵ�ǰֵ���ò������� volatile read ���ڴ����塣
     * @return ͬ��״̬�ĵ�ǰֵ
     */
    protected final int getState() {
        return state;
    }

    /**
     * ����ͬ��״̬��ֵ���ò�������volatileд���ڴ����塣
     * @param newState the new state value
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * �����ǰ״ֵ̬��������ֵ�����Զ���ͬ��״̬����Ϊ�����ĸ���ֵ���ò�������volatileд���ڴ����塣
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // ����������intrinsics������֧����һ��
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    /**
     * ������Ϊ��λ����ת�ٶ�Ҫ��ʹ��timed park�ٶȿ졣
     * ���ԵĹ��ƾ������ڷǳ��̵ĳ�ʱ����������Ӧ������
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * ���ڵ������У���Ҫʱ���г�ʼ����
     * @param node Ҫ����Ľڵ�
     * @return �ڵ��ǰ��
     */
    private Node enq(final Node node) {
        for (;;) { // ����ѭ��
            Node t = tail; // �õ�����β,��һ��Ϊnull
            if (t == null) { // ���Ϊnull������Ҫ��ʼ������
                if (compareAndSetHead(new Node())) // ����ͷ����յ�node
                    tail = head; // ����ͷ��㣬β�ڵ����ͷ���
            } else {
                node.prev = t; // ��ǰ�ڵ�ǰһ���ڵ�Ϊt����һ��Ϊ�յ�node
                if (compareAndSetTail(t, node)) { // node��Ϊβ�ڵ��滻t��tΪͷ�ڵ�
                    t.next = node; // t���¸��ڵ�Ϊnode
                    return t; // ����node��ǰ�ڵ�t
                }
            }
        }
    }

    /**
     * Ϊ��ǰ�̺߳͸���ģʽ��������ӽڵ㡣(��������null����������new Node() )
     *
     * @param mode Node.EXCLUSIVE for exclusive��null��, Node.SHARED for shared��new Node()��
     * @return �µ�node
     */
    private Node addWaiter(Node mode) { // �ѽڵ���ӵ����е�β��
        Node node = new Node(Thread.currentThread(), mode); // ���뵱ǰ�̺߳ͽڵ㣨null��new Node() ��
        // ����enq�Ŀ���·��;ʧ��ʱ���ݵ�������enq
        Node pred = tail; // pred�ڵ�Ϊβ���
        if (pred != null) { // β��㲻Ϊnull����ʼ������
            node.prev = pred; // �ڵ��ǰһ���ڵ�Ϊ֮ǰ��β���
            if (compareAndSetTail(pred, node)) { // ��node�Ľڵ���Ϊβ�ڵ�
                pred.next = node;// ������ϵ
                return node; // ���ص�ǰ�ڵ�
            }
        }
        // ��δ��ʼ����casʧ�������enq() ��ʼ������ӣ�ѭ��������
        enq(node);
        return node;
    }

    /**
     * �����е�ͷ������Ϊ�ڵ㣬�Ӷ��˳����С���ͨ��acquire�������á�
     * Ϊ�˽���GC�����Ʋ���Ҫ���źźͱ���������Ϊ�ճ�δʹ�õ��ֶΡ�
     *
     * @param node the node
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * ����node�ĺ�̽ڵ�(������ڵĻ�)��
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * ���״̬Ϊ��(��(������Ҫ�ź�)���ŷ����źš�
         * ����������ʧ�ܣ�����ͨ���ȴ��̸߳ı�״̬�����ǿ��Եġ�
         */
        int ws = node.waitStatus; // �õ��ýڵ�ĵȴ�״̬
        if (ws < 0) // ���ýڵ�״̬Ϊ���ģ����Ϊ0 ��״̬
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * ���ѵ��̱߳������ں����ڵ��У���ͨ��ֻ����һ���ڵ㡣
         * ���ǣ����ȡ������ȻΪ�գ����tail�����������ҵ�ʵ�ʵ�δȡ����̡�
         */
        Node s = node.next; // �õ�node����һ���ڵ�
        if (s == null || s.waitStatus > 0) { // ���sΪnull�������ж���
            s = null; // s��Ϊnull����ǰGC
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t; // ��tail��ǰ�������ҵ�waitStatus <= 0��
        }
        if (s != null) // ��s��Ϊ0�����Ѹ��߳�
            LockSupport.unpark(s.thread);
    }

    /**
     * �ͷŹ���ģʽ�Ķ��������źź���ߣ���ȷ��������
     * (ע��:���ڶ�ռģʽ���ͷ��൱�ڵ���unpark�����˵�ͷ���������Ҫ�źš�)
     */
    private void doReleaseShared() {
        /*
         * ȷ��һ���汾��������ʹ���������ڽ��еĻ�ȡ/���������ǰ���ͨ���ķ�ʽ���еģ�
         * �������Ҫ�źŵĻ����ͻ���ͼж��head�ĺ���ߡ�
         * �����û�У���״̬����ΪPROPAGATE����ȷ���ڷ����󴫲��Խ�������
         * ���⣬���Ǳ���ѭ�����Է���ִ�д˲���ʱ����½ڵ㡣
         * ���⣬��unpark�����˵�������;��ͬ��������Ҫ֪��CAS�Ƿ�λʧ�ܣ��Ƿ����¼�顣
         */
        for (;;) { // ����ѭ��
            Node h = head; // �õ�ͷ����ʼ��֮����new Node
            if (h != null && h != tail) { // �ж϶��������еȴ��߳�
                int ws = h.waitStatus; // �õ��ȴ�״̬
                if (ws == Node.SIGNAL) { // ��״̬Ϊ-1��������
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) // �滻��ǰ״̬Ϊ0
                        continue;             // ��ʧ�ܼ���ѭ��
                    unparkSuccessor(h); // ���û��Ѻ����߳�
                } // �ǵ�����״̬����wsΪ0��״̬�����滻Ϊ-3�����Ѻ����߳�
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // ��ʧ�ܼ���ѭ��
            }
            if (h == head)                   // ��ͷѭ��
                break; // ����ı䣬����ѭ��
        }
    }

    /**
     * ���ö��е�ͷ�����������������Ƿ��ڹ���ģʽ�µȴ�������ǣ��򴫲��������> 0�򴫲�״̬�����á�
     *
     * @param node the node
     * @param propagate �ӻ�ȡ�������ķ���ֵ
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // ���潫��¼��ͷ���м��
        setHead(node); // ����nodeΪͷ�ڵ�
        /*
         * �����ź���һ���Ŷӽڵ����:
         *   �����ɵ�����ָʾ,
         *     �򱻼�¼(��Ϊh.waitStatus ֮ǰ��֮��ĵȴ�״̬)
         *     (ע��:�⽫ʹ�õȴ�״̬���źż�飬��Ϊ����״̬���ܻ�ת��Ϊ�źš�)
         * and
         *   ��һ���ڵ��ڹ���ģʽ�µȴ����������ǲ�֪������Ϊ���������ǿյ�
         *
         * �����ּ��ı����Կ��ܻᵼ�²���Ҫ�Ļ��ѣ���ֻ�����ж�����ٻ��/�ͷ�ʱ�Ż�������
         * ���Դ���������ڻ�ܿ����Ҫ�źš�
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            Node s = node.next; // ��ǰ�ڵ����һ���ڵ�
            if (s == null || s.isShared()) // ��Ϊnull�����ǹ���״̬��nextWaiter == new Node()��
                doReleaseShared(); // �ͷŹ�����
        }
    }

    // ���ְ汾��ȡ��ʵ�ó���

    /**
     * ȡ�����ڽ��еĻ�ȡ���ԡ�
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // ����ڵ㲻���������
        if (node == null)
            return;

        node.thread = null; // �ڵ��߳���Ϊnull

        // ����ȡ����ǰ��
        Node pred = node.prev;
        while (pred.waitStatus > 0) // ǰ�ε�״̬����0�Ļ���������������ѭ��
            node.prev = pred = pred.prev;

        // predNext��Ҫunsplice�����Խڵ㡣����������ʧ�ܣ�������ǣ�
        // ����������£�����ʧȥ�˾�������һ��ȡ�����źţ�����û�н�һ�����ж��Ǳ�Ҫ�ġ�
        Node predNext = pred.next;

        // �������ʹ��������д����CAS��
        // �����ԭ�Ӳ���֮�������ڵ�����������ǡ�
        // �ڴ�֮ǰ�����ǲ��������̵߳ĸ��š�
        node.waitStatus = Node.CANCELLED;

        // ��������һ�����Ƴ�
        if (node == tail && compareAndSetTail(node, pred)) { // �滻Ϊǰһ��
            compareAndSetNext(pred, predNext, null); // ��pred��һ������Ϊnull��node���ѹ�ϵ
        } else {
            // �����������Ҫ�źţ���������pred����һ������
            // ��������õ�1������������ֳ��
            int ws;
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) { // �޳����߳�
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node); // ���û��ǰ����̣߳����Ѻ���Ľڵ�
            }

            node.next = node; // help GC
        }
    }

    /**
     * ���͸���δ�ܻ�ȡ�Ľڵ��״̬������߳�����������true��
     * �������вɼ���·�����źſ��ơ���Ҫpred == node.prev��
     *
     * @param pred �ڵ��ǰ�α��ֵ�״̬
     * @param node the node
     * @return {@code true} ����߳�����
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // ��ȡʧ�ܺ�����ס
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * ����ڵ��Ѿ������������ͷ��źŵ�״̬�����������԰�ȫ��������
             */
            return true; // ��ǰ�ڵ�����ס����ýڵ�Ҳ����
        if (ws > 0) {
            /*
             * ���ǰ���ж��˻�ȡ���ˣ�����
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * �ȴ�״̬����Ϊ0�򴫲�����ָʾ������Ҫ�źţ�����Ҫ�����������߽���Ҫ���ԣ���ȷ�������ܻ��������֮ǰ��
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * ����ķ����жϵ�ǰ�̡߳�
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * �����������ټ���Ƿ��ж�
     *
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * ��ȡ��ʽ���ֶ�����������/����ģʽ�Ϳ���ģʽ��
     * ÿһ�ֶ���ͬС�죬������������ز�ͬ��
     * �����쳣����(����ȷ��������tryAcquire�׳��쳣ʱȡ��)���������ƵĽ������ã�
     * ֻ�к��ٵ������ǿ��ܵģ����ٲ�����������̫����𺦡�
     */

    /**
     * ���ڶ����е��߳��Զ�ռ�Ĳ����ж�ģʽ��ȡ�����������ȴ������Լ���ȡ��
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) { // ����
                final Node p = node.predecessor(); // �õ��ڵ�ǰ��
                if (p == head && tryAcquire(arg)) { // ǰ�����Ϊhead�Ļ������ѵ�ǰ�ڵ�
                    setHead(node); // �ɹ����ѣ��ѵ�ǰ�ڵ���Ϊͷ���
                    p.next = null; // help GC
                    failed = false;
                    return interrupted; // �����ж�״̬
                }
                if (shouldParkAfterFailedAcquire(p, node) && // ��ȡʧ������ס
                    parkAndCheckInterrupt()) // ����ж�
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * �������ж�ģʽ��ȡ��
     * @param arg the acquire argument
     */
    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE); // ���������
        boolean failed = true;
        try {
            for (;;) { // ����
                final Node p = node.predecessor(); // �õ�ǰ��
                if (p == head && tryAcquire(arg)) { // ����ͷ��㲢�һ�ȡ����
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) && // ��ȡʧ������
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * ����ʱ��ģʽ��ȡ����
     *
     * @param arg ��ȡ����
     * @param nanosTimeout ���ȴ�ʱ��
     * @return {@code true} �����ȡ��
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE); // ������
        boolean failed = true;
        try {
            for (;;) { // ����
                final Node p = node.predecessor(); // ǰ��
                if (p == head && tryAcquire(arg)) { // ��ȡ��
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) // �ж��Ƿ�ʱ
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) && // ��ȡʧ��
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * �ڹ�������ģʽ�»�ȡ
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED); // ��������ӵ�����
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) { // ����
                final Node p = node.predecessor(); // �õ�ǰ��
                if (p == head) { // ����ͷ���
                    int r = tryAcquireShared(arg); // ��ͼ��ȡ��
                    if (r >= 0) { // ������0������ͷ�ʹ�����
                        setHeadAndPropagate(node, r); // ����ͷ�ʹ�����
                        p.next = null; // help GC
                        if (interrupted) // �����ж�״̬���ж�
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * �ڹ����ж�ģʽ�»�ȡ
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) { // ����
                final Node p = node.predecessor(); // �õ�ǰ��
                if (p == head) { // ��ǰ��Ϊͷ�����ȡ��
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * �ڹ���ʱ��ģʽ�»�ȡ
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // Main exported methods

    /**
     * �����Զ�ռģʽ��ȡ���÷���Ӧ�ò�ѯ�����״̬�Ƿ������Զ�ռģʽ��ȡ�������������Ӧ�û�ȡ����
     *
     * <p>ִ�л�ȡ���߳����ǵ��ô˷���������˷�������ʧ�ܣ����ȡ�������ܻ���߳̽����Ŷ�(�������û���Ŷ�)��
     * ֱ��ͨ�������̵߳��ͷŷ����źš����������ʵ��Lock.tryLock()������
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg ��ȡ���������ֵ���Ǵ��ݸ�һ����ȡ������ֵ���������ڽ���һ������waitʱ�����ֵ��
     *            ��ֵ��δ���͵ģ����Ա�ʾ��ϲ�����κ����ݡ�
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * ��ͼ��״̬����Ϊ�Զ�ռģʽ��ӳ������
     *
     * <p>ִ��release���߳����ǵ��ô˷�����
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * �����Թ���ģʽ��ȡ���÷���Ӧ�ò�ѯ�����״̬�Ƿ������ڹ���ģʽ�»�ȡ�������������Ӧ�û�ȡ����
     *
     * <p>ִ�л�ȡ���߳����ǵ��ô˷���������˷�������ʧ�ܣ����ȡ�������ܻ���߳̽����Ŷ�(�������û���Ŷ�)��
     * ֱ��ͨ�������̵߳��ͷŷ����źš�
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return ʧ��Ϊ��ֵ;�������ģʽ�µĻ�ȡ�ɹ�������û�к����Ĺ���ģʽ��ȡ���Գɹ���
     * ��Ϊ��;����ڹ���ģʽ�»�ȡ�ɹ���
     * �������Ĺ���ģʽ��ȡҲ���ܳɹ�����ô����������£������ĵȴ��̱߳���������ԡ�
     * (֧�����ֲ�ͬ�ķ���ֵ��ʹ�˷����������ڽ���ĳЩ����²Ž��л�ȡ���������С�)�ɹ�֮���������ͻ���ˡ�
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * ���Խ�״̬����Ϊ�Թ���ģʽ��ӳ������
     *
     * <p>ִ��release���߳����ǵ��ô˷�����
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * �������Ե�ǰ(����)�̱߳���ͬ�����򷵻�true���˷�����ÿ�ε���δ�ȴ���AbstractQueuedSynchronizerʱ���á�
     * ConditionObject������(�ȴ���������release(int)��)
     *
     * <p>Ĭ��ʵ�ֽ��׳�UnsupportedOperationException��
     * �˷�������AbstractQueuedSynchronizer�ڲ����á����û��ʹ���������򲻱ض����������󷽷���
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * ������ģʽ�л�ȡ��������ϡ�ͨ�����һ�ε���tryAcquire(int)ʵ�֣����سɹ���
     * �����߳��ڶ����У������ظ������ͻ��ѣ�����tryAcquire(int)ֱ���ɹ������������������ʵ�ַ���Lock.lock()��
     *
     * @param arg ��ȡ�Ĳ��������ֵ���ݸ�tryAcquire(int)�������Ƿ���ϵĿ��Դ�����ϲ�����˺��¡�
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

    /**
     * �Զ�ռģʽ��ȡ������ж�����ֹ�����ȼ���ж�״̬��Ȼ�����ٵ���һ��tryAcquire(int)��
     * �ɹ��󷵻ء������߳̽��Ŷӣ����ܻ��ظ�������ȡ������������tryAcquire(int)��
     * ֱ���ɹ����̱߳��жϡ����������������ʵ��Lock.lockInterruptibly()������
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg);
    }

    /**
     * �����Զ�ռģʽ��ȡ������жϽ���ֹ�����������ʱ��ʱ��ʧ�ܡ�
     * ���ȼ���ж�״̬��Ȼ�����ٵ���һ��tryAcquire(int)���ɹ��󷵻ء�
     * �����߳̽��Ŷӣ����ܻ��ظ�������ȡ������������tryAcquire(int)��
     * ֱ���ɹ����߳��жϻ�ʱ�������˷���������ʵ�ַ�������TimeUnit tryLock(long)��
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
            doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * �Զ�ռģʽ���������tryRelease(int)����true��
     * ��ͨ�����һ�������̵߳�������ʵ�֡����������������ʵ��Lock.unlock()������
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @return the value returned from {@link #tryRelease}
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * �����жϣ��Թ���ģʽ��ȡ�����ȵ�������һ��tryacquiremred (int)��
     * �ɹ��󷵻ء������߳̽��Ŷӣ����ܻᷴ��������ȡ������������tryacquiremred (int)ֱ���ɹ���
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

    /**
     * �Թ���ģʽ��ȡ������жϽ���ֹ�����ȼ���ж�״̬��Ȼ�����ٵ���һ��tryacquiremrered (int)��
     * �ɹ��󷵻ء������߳̽��Ŷӣ����ܻ��ظ�������ȡ������������tryacquiremred (int)��
     * ֱ���ɹ����̱߳��жϡ�
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * �����Թ���ģʽ��ȡ������жϽ���ֹ�����������ʱ��ʱ��ʧ�ܡ����ȼ���ж�״̬��
     * Ȼ�����ٵ���һ��tryacquiremrered (int)���ɹ��󷵻ء������߳̽��Ŷӣ�
     * ���ܻ��ظ�������ȡ������������tryacquiremred (int)��ֱ���ɹ����߳��жϻ�ʱ������
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
            doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * �Թ���ģʽ���������tryReleaseShared(int)����true����ͨ��ȡ������һ�������߳���ʵ�֡�
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods

    /**
     * ��ѯ�Ƿ����߳����ڵȴ���ȡ��ע�⣬�����жϺͳ�ʱ���µ�ȡ���������κ�ʱ������
     * ���������ķ��ز�����֤�κ������߳̽���á�
     *
     * <p>�ڴ�ʵ���У��˲����Գ���ʱ�䷵�ء�
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * ��ѯ�Ƿ����κ��߳����ù���ͬ����;Ҳ����˵�����һ����ȡ����������������
     *
     * <p>�ڴ�ʵ���У��˲����Գ���ʱ�䷵�ء�
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * ���ض����еĵ�һ��(�ȴ�ʱ�����)�̣߳������ǰû���߳��Ŷӣ��򷵻�null��
     *
     * <p>�ڴ�ʵ���У��˲���ͨ���Գ���ʱ�䷵�أ�����������߳�ͬʱ�޸Ķ��У������������ʱ���е�����
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * fastpathʧ��ʱ���õ�getFirstQueuedThread�汾
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * ��һ���ڵ�ͨ����head.next�����Ի�ȡ�����߳��ֶΣ�ȷ��һ�µĶ�ȡ:����߳��ֶ�Ϊ�ջ�s��
         * prev������head��Ȼ��һЩ�����߳��ڶ�ȡ֮�䲢����ִ��setHead���ڽ��б���֮ǰ�����ǳ��������Ρ�
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head����һ���ֶο��ܻ�û�����ã����߿�����setHead֮��ȡ�������á�
         * �������Ǳ�����һ��tail�ǲ��ǵ�һ���ڵ㡣���û�У����Ǽ�������β����ͷ����ȫ���ҵ���һ����֤��ֹ��
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * ��������̵߳�ǰ�����Ŷӣ��򷵻�true��
     *
     * <p>��ʵ�ֱ���������ȷ�������̵߳Ĵ��ڡ�
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * �����һ���Ŷӵ��߳�(�������)������ģʽ�ȴ����򷵻�{@code true}��
     * ��������������{@code true}�����ҵ�ǰ�߳����ڳ����Թ���ģʽ��ȡ
     * (Ҳ����˵����������Ǵ�{@link # tryacquiresred}���õ�)��
     * ��ô���Ա�֤��ǰ�̲߳��ǵ�һ���Ŷӵ��̡߳�����ReentrantReadWriteLock����������ʽ��
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    /**
     * ��ѯ�Ƿ����κ��̵߳ȴ���ȡ��ʱ��ȵ�ǰ�̳߳���
     *
     * <p>�˷����ĵ����൱��(�����ܱ�):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * <p>ע�⣬�����жϺͳ�ʱ���µ�ȡ��������ʱ������
     * ���������ķ��ز�����֤�����̻߳��ڵ�ǰ�߳�֮ǰ��á�
     * ͬ�����ڴ˷�������false�����ڶ����ǿյģ���һ���߳�Ҳ����Ӯ����ӡ�
     *
     * <p>�÷������������һ����ƽ��ͬ�������Ա�����ײ��
     * ����һ��ͬ������tryAcquire(int)����Ӧ�÷���false��
     * ��������������true(��������һ�������ȡ)��
     * ��ô����tryacquired (int)����Ӧ�÷���һ����ֵ��
     * ���磬һ����ƽ�ģ�������ģ���ռģʽͬ������tryAcquire����������������:
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods

    /**
     * ���صȴ���ȡ���߳������Ĺ���ֵ�����ֵֻ��һ������ֵ����Ϊ��������������ڲ����ݽṹʱ��
     * �̵߳��������ܻᶯ̬�仯���÷������ڼ��ϵͳ״̬��������ͬ�����ơ�
     *
     * @return the estimated number of threads waiting to acquire
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * ����һ�������������ڵȴ���ȡ���̵߳ļ��ϡ�
     * ��Ϊ�ڹ���������ʱ��ʵ�ʵ��̼߳����ܻᶯ̬�仯��
     * ���Է��صļ���ֻ��һ�����Ч���Ĺ��ơ����ؼ��ϵ�Ԫ��û���ض���˳��
     * ���ַ�����Ŀ����Ϊ�˷��㹹���ṩ���㷺�ļ�����ʩ�����ࡣ
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * ����һ�����ϣ����а����������ڵȴ��Զ�ռģʽ��ȡ���̡߳�
     * ��������getQueuedThreads()��ͬ�����ԣ�ֻ����ֻ������Щ���ڶ�ռ��ȡ���ȴ����̡߳�
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * ����һ�����ϣ����а��������ڹ���ģʽ�µȴ���ȡ���̡߳�
     * ��������getQueuedThreads()��ͬ�����ԣ�ֻ����ֻ������Щ���ڹ����ȡ���ȴ����̡߳�
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * ���ر�ʶ��ͬ��������״̬���ַ������������е�״̬�����ַ�����state =����
     * ���getState()�ĵ�ǰֵ���Լ���nonempty����empty������ȡ���ڶ����Ƿ�Ϊ�ա�
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     * �ڵ㡣prev�����Ƿǿյģ��������ܷ��ڶ����ϣ���Ϊ�������ڶ����ϵ�ca���ܻ�ʧ�ܡ�
     * �������Ǳ����β����ʼ������ȷ����ȷʵ�ɹ��ˡ��ڶ���������ĵ����У���������β������������CASʧ��(���ǲ�̫���ܵ�)���������������������Ǽ����������̫�ࡣ
     * @param node the node
     * @return true if is reacquiring
     */
    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }

    /**
     * ����ڵ��β���������ͬ�����У�����true��ֻ��isOnSyncQueue��Ҫʱ���á�
     * @return true if present
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /**
     * ���ڵ����������ת�Ƶ�ͬ�����С�����ɹ�����true��
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     */
    final boolean transferForSignal(Node node) {
        /*
         * ����޷����ĵȴ�״̬����ڵ��ѱ�ȡ����
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         *  ƴ�ӵ����У�����������ǰ���ĵȴ�״̬���Ա����߳�(����)���ڵȴ���
         * ���ȡ�������õȴ�״̬ʧ�ܣ����Ѳ�����ͬ��(����������£��ȴ�״̬������ʱ���󣬵���������κ��˺�)��
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * ����ڵ㣬�����Ҫ����һ��ȡ���ĵȴ���ͬ�����С�����߳��ڱ������ź�֮ǰ��ȡ�����򷵻�true��
     *
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /*
         * ������Ƕ�ʧ��һ��saignal()����ô���ǾͲ��ܼ�����ֱ�����������enq()��
         * �ڲ���ȫת�ƹ�����ȡ���Ǻ����ģ�Ҳ�Ƕ��ݵģ�����ֻҪ��ת�Ϳ����ˡ�
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     * @param node the condition node for this wait
     * @return previous sync state
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState(); // ��ǰ��״̬
            if (release(savedState)) { // �ͷŵ�ǰ����������һ���߳�
                failed = false;
                return savedState; // ����state
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions

    /**
     * ��ѯ���������������Ƿ�ʹ�ô�ͬ������Ϊ������
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * ��ѯ�Ƿ����߳����ڵȴ����ͬ���������ĸ���������
     * ע�⣬��Ϊ��ʱ���жϿ�����ʱ���������������ķ��ز�����֤�������źŽ������κ��̡߳�
     * �÷�����Ҫ���ڼ��ϵͳ״̬��
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * �������ͬ���������ĸ��������ϵȴ����߳����Ĺ���ֵ��
     * ��ע�⣬���ڳ�ʱ���жϿ�����ʱ��������˹���ֵ����Ϊʵ�ʵȴ������������ޡ�
     * �˷���������ڼ���ϵͳ״̬������������ͬ�����ơ�
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * ����һ�����ϣ����а����������ڵȴ����ͬ���������ĸ����������̡߳�
     * ��Ϊ�ڹ���������ʱ��ʵ�ʵ��̼߳����ܻᶯ̬�仯�����Է��صļ���ֻ��һ�����Ч���Ĺ��ơ�
     * ���ؼ��ϵ�Ԫ��û���ض���˳��
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * ��Ϊ{@link��}ʵ�ֵĻ�����{@link AbstractQueuedSynchronizer}������ʵ�֡�
     *
     * <p>�����ķ����ĵ��������ƣ������Ǵ����������û��ĽǶ���������Ϊ�淶��
     * �����ĵ����汾ͨ����Ҫ���������������ڹ�����{@code AbstractQueuedSynchronizer}������������ĵ���
     *
     * <p>������ǿ����л��ģ����������ֶζ���˲̬�ģ���˷����л�������û�еȴ��ߡ�
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** �������е�һ���ڵ� */
        private transient Node firstWaiter;
        /** �����������һ���ڵ� */
        private transient Node lastWaiter;

        /**
         * ����һ���µ� {@code ConditionObject} ʵ��.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * ���һ���µĵȴ����С�
         * @return its new wait node
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // ������һ���ȴ�ȡ���ˣ����
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * ɾ���ʹ���ڵ㣬ֱ�����в���ȡ����һ����null��
         * ��signal�з������������ԭ����Ϊ�˹�����������û�еȴ����������������
         * @param first (non-null) the first node on condition queue
         */
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }

        /**
         * ɾ���ʹ������нڵ㡣
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * ����������ȡ����ȡ�������߽ڵ�����ӡ�ֻ���ڳ�����ʱ�ŵ��á�
         * ���������ȴ��ڼ䷢��ȡ��ʱ���ã��ڿ���lastWaiter�ѱ�ȡ��ʱ������waiterʱ���á�
         * ��Ҫʹ�ô˷�����������û���ź�ʱ��������������ˣ���ʹ��������Ҫ�����ı�����
         * Ҳֻ����û���źŵ�����·�����ʱ��ȡ��ʱ�����Ż������á����������нڵ㣬
         * ������ͣ��һ���ض���Ŀ���ϣ�ȡ��ָ�������ڵ������ָ������ӣ�������Ҫ��ȡ���籩�ڼ���ж�����±�����
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    trail = t;
                t = next;
            }
        }

        // ��������

        /**
         * ����ȴ��߳�(�������)�Ӹ������ĵȴ������ƶ���ӵ�����ĵȴ����С�
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signal() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignal(first);
        }

        /**
         * ���������µ������̴߳ӵȴ������ƶ���ӵ�����ĵȴ����С�
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * ʵ�ֲ����ж������ȴ���
         * <ol>
         * <li> ������{@link #getState}���ص���״̬��
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        private void reportInterruptAfterWait(int interruptMode)
            throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * ʵ���ж������ȴ���
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * ʵ�ֶ�ʱ�����ȴ���
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * ʵ���˾��Զ�ʱ�����ȴ���
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * ʵ�ֶ�ʱ�����ȴ���
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * ������������ɸ�����ͬ�����󴴽��ģ��򷵻�true��
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * ��ѯ�Ƿ����߳��ڵȴ���������ʵ����{@link AbstractQueuedSynchronizer # hasWaiters (ConditionObject)}��
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * ���صȴ����������߳����Ĺ�������
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * ����һ�����ϣ����а����������ڵȴ����������̡߳�
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * ������֧�ֱȽϺ����á�������Ҫ�����ﱾ��ʵ����һ��:Ϊ������������ǿ��
     * ���ǲ�����ʽ�����໯AtomicInteger��������������Ч�����õġ�
     * ��ˣ�����ʹ��hotspot intrinsics API����ʵ�֣��Լ���������Ĳ��㡣
     * ������������ʱ�����Ƕ�����CASable�ֶ���ͬ��������(�������ԭ���ֶθ��³��������)��
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
