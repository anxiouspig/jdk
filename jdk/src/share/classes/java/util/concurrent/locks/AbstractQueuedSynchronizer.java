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
 * 提供一个框架来实现基于先进先出(FIFO)等待队列的阻塞锁和相关同步器(信号量、事件等)。
 * 这个类被设计成大多数类型的同步器的有用基础，这些同步器依赖于单个原子int值来表示状态。
 * 子类必须定义改变这个状态的受保护的方法，这些方法定义了这个状态对于被获取或释放的对象意味着什么。
 * 考虑到这些，这个类中的其他方法执行所有的排队和阻塞机制。
 * 子类可以维护其他状态字段，但是只有使用getState()、setState(int)和
 * compareAndSetState(int, int)方法自动更新的int值才会被同步跟踪。
 *
 * <p>子类应该定义为非公共的内部帮助类，用于实现其封闭类的同步属性。
 * 类AbstractQueuedSynchronizer不实现任何同步接口。
 * 相反，它定义了像acquireInterruptibly(int)这样的方法，
 * 这些方法可以被具体的锁和相关的同步器适当地调用来实现它们的公共方法。
 *
 * <p>此类支持默认独占模式和共享模式中的一种或两种。
 * 当以独占模式获取时，其他线程尝试的获取将无法成功。
 * 由多个线程获取的共享模式可能(但不一定)成功。这个类并不“理解”这些差异，
 * 除非在机械意义上，即当共享模式获取成功时，
 * 下一个等待的线程(如果存在的话)也必须确定它是否也可以获取。
 * 在不同模式下等待的线程共享相同的FIFO队列。
 * 通常，实现子类只支持其中一种模式，但这两种模式都可以发挥作用，例如在ReadWriteLock中。
 * 只支持独占或共享模式的子类不需要定义支持未使用模式的方法。
 *
 * <p>这个类定义了一个嵌套的AbstractQueuedSynchronizer.ConditionObject类,
 * 可以用作条件由子类实现支持独占模式的方法isHeldExclusively()报告同步是否只对当前线程持有,
 * release(int)方法调用与当前getState()值完全释放该对象,并acquire(int),
 * 鉴于这个保存的状态值,最终获得的该对象恢复到以前的状态。
 * AbstractQueuedSynchronizer方法不会创建这样的条件，所以如果不能满足这个约束，
 * 就不要使用它AbstractQueuedSynchronizer的行为。
 * 当然，条件对象取决于其同步器实现的语义。
 *
 * <p>该类提供内部队列的检查、检测和监视方法，以及条件对象的类似方法。
 * 可以根据需要使用AbstractQueuedSynchronizer将它们导出到类中，以实现同步机制。
 *
 * <p>这个类的序列化只存储底层的原子整数维护状态，因此反序列化对象有空线程队列。
 * 需要序列化的典型子类将定义一个readObject方法，在反序列化时将其恢复到已知的初始状态。
 *
 * <h3>使用</h3>
 *
 * <p>为了使用这个类作为同步的基础，阅读以下的方法，
 * 适用通过查看/修改同步状态通过getState()，setState(int)，
 * and/or compareAndState(int, int)：
 *
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}
 * </ul>
 *
 * 这些方法的每一个都会抛出UnsupportedOperationException。
 * 实现这些方法必须内部实现线程安全，应该大体上剪短非阻塞的。
 * 定义这些方法仅支持使用这些类。其他方法声明final因为它们不能独立变化。
 *
 * <p>你可能也发现从aqs继承的方法使用保证线程拥有一个排它锁。
 * 你可以使用他们，这可能检查和诊断工具去帮助用户决定线程持有锁。
 *
 * <p>即使这个类是内部先进先出队列为基础，不会自动的先进先出协议。
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>如果没入队，则入队线程</em>;
 *        <em>可能阻塞当前线程</em>;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>唤醒第一个队列线程</em>;
 * </pre>
 *
 * (共享模式是相同的但是可能调用级联信号)
 *
 * <p id="barging">因为检查获取被调用在入队前，
 * 一个新的获取锁的线程可能抢在别人前面阻塞和入队。
 * 然而，如果要求，你可以定义tryAcquire 和 tryAcquireShared
 * 去禁用通过调用一个或多个观察方法，因此提供一个公平的先进先出的获得顺序。
 * 特别的，更多的公平同步可以定义为tryAcquire去返回false如果
 * hasQueuedPredecessor()（一个方法特别设计去用于公平实现）返回true。
 * 其他变量时可能的。
 *
 * <p>吞吐量和可伸缩性通常在默认的barging
 * (也称为贪心策略、放弃策略和convoey -avoidance)策略中是最高的。
 * 虽然不能保证这是公平的或无饥饿的，但允许较早的队列线程在较晚的队列线程之前进行重新争用，
 * 并且每个重新争用都有无偏倚的机会成功地对抗传入线程。
 * 另外，虽然获取并不像通常意义上那样“旋转”，但它们可能执行多次tryAcquire调用，
 * 并在阻塞之前穿插其他计算。当独占性同步只短暂持有时，这就提供了旋转的大部分好处，
 * 而当独占性同步不持有时，则没有大部分责任。
 * 如果需要，您可以通过前面的调用来获取带有“快速路径”检查的方法来增强这种能力，
 * 可能需要预先检查hascontend()和/或hasQueuedThreads()，以便仅在同步器可能不存在争用时才这样做。
 *
 * <p>这个类提供一个高效的和部分同步的基础通过指定同步的使用范围可以依赖它的
 * state,acquire,release参数，和一个内部的先进先出的队列。当这不够用时，
 * 你可以建立同步表一个低水平使用actomic类，你用消费队列和Lockpark阻塞支持。
 *
 * <h3>Usage Examples</h3>
 *
 * <p>这是一个非重入的排它锁类，用0代表非锁状态，1去代表锁定状态。
 * 当一个非重入锁不能严格获得当前线程，这个类用任何方式去简便监视。
 * 它也支持条件和暴露基础方法的一个。
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // 我们内部的帮助类
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // 表示是否锁定状态
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // 如果状态为0则可以获取锁
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // 其他  不使用
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // 设置状态为0，释放锁
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // 提供一个Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // 恰当地并行
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // 从新解锁状态
 *     }
 *   }
 *
 *   // 同步对象做了所有艰难的工作
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
 * <p>这是一个占有类像CountDownLatch，它仅仅要求一个单个信号。
 * 因为一个占有锁是非排他的，它用来共享获取和释放方法。
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
     * 创建一个初始同步状态为0的新{@code AbstractQueuedSynchronizer}实例。
     */
    protected AbstractQueuedSynchronizer() { }

    /**
     * 等待队列节点类。
     *
     * <p>等待队列是“CLH”(Craig、Landin和Hagersten)锁队列的变体。
     * CLH锁通常用于自旋锁。相反，我们使用它们来阻塞同步器，但是使用相同的基本策略，
     * 即在其节点的前身中保存关于线程的一些控制信息。每个节点中的“status”字段跟踪线程是否应该阻塞。
     * 一个节点在它的前一个节点释放时发出信号。队列的每个节点作为一个特定通知样式的监视器，
     * 它持有一个等待线程。状态字段不控制线程是否被授予锁等。如果一个线程是队列中的第一个线程，
     * 它可能会尝试获取它。但是成为第一并不能保证成功;它只给予人们竞争的权利。
     * 因此，当前发布的竞争者线程可能需要重新等待。
     *
     * <p>要加入到CLH锁中，您需要原子性地将其作为新尾剪接进来。
     * 要退出队列，只需设置head字段。
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>将nsertion放入CLH队列只需要在“tail”上执行一个原子操作，
     * 因此从unqueued到queued有一个简单的原子分界点。
     * 类似地，退出队列只涉及更新“head”。但是，节点需要做更多的工作来确定谁是他们的继任者，
     * 部分原因是为了处理超时和中断可能导致的取消。
     *
     * <p>“prev”链接(在原来的CLH锁中没有使用)主要用于处理取消。
     * 如果一个节点被取消，它的后继者(通常)会被重新链接到一个没有取消的前继者。
     *
     * <p>我们还使用“next”链接来实现阻塞机制。每个节点的线程id都保存在自己的节点中，
     * 因此前一个节点通过遍历下一个链接来通知下一个节点唤醒，以确定它是哪个线程。
     * 确定后继者必须避免使用新加入队列的节点来设置其前辈的“下一个”字段。
     * 当节点的后继节点为空时，通过从原子更新的“尾部”向后检查来解决这个问题。
     * (或者，换句话说，下一个链接是一种优化，因此我们通常不需要反向扫描。)
     *
     * <p>对消在基本算法中引入了一些保守性。因为我们必须轮询是否取消其他节点，
     * 所以我们可能会忽略被取消的节点是在前面还是在后面。解决这一问题的方法是，
     * 总是在取消后取消继任者，让他们稳定在新的前任上，除非我们能够确定一个未取消的前任来承担这一责任。
     *
     * <p>CLH队列需要一个虚构的头节点来启动。但我们不会在施工时创建它们，
     * 因为如果从来没有争用，就会浪费精力。相反，在第一次争用时构造节点并设置头和尾指针。
     *
     * <p>等待条件的线程使用相同的节点，但使用额外的链接。
     * 条件只需要在简单(非并发)链接队列中链接节点，
     * 因为它们只在独占时被访问。在等待时，节点被插入到条件队列中。
     * 收到信号后，节点被转移到主队列。状态字段的特殊值用于标记节点所在的队列。
     *
     * <p>感谢Dave Dice、Mark Moir、Victor Luchangco、Bill Scherer和Michael Scott，
     * 以及JSR-166专家组的成员，他们对这个类的设计提供了有用的想法、讨论和批评。
     */
    static final class Node {
        /** 共享模式下等待的节点 */
        static final Node SHARED = new Node();
        /** 排他模式下等待的节点 */
        static final Node EXCLUSIVE = null;

        /** 表示线程取消 */
        static final int CANCELLED =  1;
        /** 后续线程需要阻塞 */
        static final int SIGNAL    = -1;
        /** 条件状态下线程需要阻塞 */
        static final int CONDITION = -2;
        /**
         * 下一次获取共享锁会无限制传播
         */
        static final int PROPAGATE = -3;

        /**
         * 状态字段:
         *
         *   SIGNAL:     此节点的后续节点（或将成为后续节点）会阻塞（通过park），
         *               所以当它释放或者取消的时候，当前节点必须后续节点。为了避免竞争，
         *               获取方法必须判断它们需要一个信号量，然后重新原子性的获取，之后，在失败情况下会阻塞。
         *
         *   CANCELLED:  由于超时或中断，当前节点会取消。节点不会离开此状态。特别的，取消的线程不会再次阻塞。
         *
         *   CONDITION:  此节点当前是条件队列。除非status设置为0的时候，否则它不会是同步队列。
         *              （这个值在这的使用不会做字段的其他用途，仅仅简化结构）
         *
         *   PROPAGATE:  一个释放共享锁应该传递给其他节点。在doReleaseShared（仅对于头结点）设置为了确保传播继续，
         *              即使其他操作已经中断了。
         *
         * 0：以上都不是。
         *
         * 这些值用来简化使用。非负数值意味着一个节点不需要阻塞。所以，大多数节点不需要去检查特别的值，仅作为信号。
         *
         * 这个字段对于正常同步节点初始化为0，对于条件节点为-2。用cas修改（如果可能，volatile写入）
         */
        volatile int waitStatus;

        /**
         * 当前 节点/线程 依赖于为了检查状态连接到前任的节点，进入队列时指定，出队时设为null（为了GC）。
         * 所以，一个后续的取消，当找到一个非取消的节点时，我们跳过已经离开的取消的节点。
         * 因为头结点不会取消：一个节点变为头结点只会在重新获得锁的时候。一个取消的线程不会成功获得锁。
         * 一个线程只会取消它自己，不会取消其他节点。
         */
        volatile Node prev;

        /**
         * 当前节点或线程在释放的时候唤醒的连接继任的节点。
         * 入队时指定，调整取消的前任，，出队时设为null（为了GC）。
         * 入队操作没有指定前任的下一个字段，所以看见下一个字段为null不意味着到了队列尾。
         * 取消节点的下一个字段设置为它自己而不失null，生命易于同步队列。
         */
        volatile Node next;

        /**
         * 节点入队的线程。构造时初始化，使用后设为null。
         */
        volatile Thread thread;

        /**
         * 条件或特殊值共享连接的下一个等待节点，因为条件队列仅进入当持有排它锁，
         * 我们只需要一简单的连接队列去持有节点当条件等待时。
         * 他们之后会转移队列去重新获取。因为条件锁是排他的，我们保存一个字段用特殊值去指定共享模型。
         */
        Node nextWaiter;

        /**
         * 如果节点在共享模式下等待，则返回true。
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * 返回之前的节点，或者在null的时候抛出NullPointterException。
         * 当前任不为null时使用。检查null可以省略，但是可以帮助VM。
         *
         * @return 节点的前任
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            // 如果前一个节点为null则抛异常，否则返回
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // 用于初始化头和共享的标志
        }

        Node(Thread thread, Node mode) {     // 用于添加等待者
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Condition使用
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * 等待队列的头部，懒加载初始化。除了初始化，仅会通过方法setHead修改。
     * 注意：如果头存在了，等待状态不保证会取消。
     */
    private transient volatile Node head;

    /**
     * 等待队列的尾部，延迟初始化。仅通过方法enq修改以添加新的等待节点。
     */
    private transient volatile Node tail;

    /**
     * 同步状态。
     */
    private volatile int state;

    /**
     * 返回同步状态的当前值。该操作具有 volatile read 的内存语义。
     * @return 同步状态的当前值
     */
    protected final int getState() {
        return state;
    }

    /**
     * 设置同步状态的值。该操作具有volatile写的内存语义。
     * @param newState the new state value
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * 如果当前状态值等于期望值，则自动将同步状态设置为给定的更新值。该操作具有volatile写的内存语义。
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // 请参阅下面的intrinsics设置以支持这一点
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    /**
     * 以纳秒为单位的旋转速度要比使用timed park速度快。
     * 粗略的估计就足以在非常短的超时情况下提高响应能力。
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 将节点插入队列，必要时进行初始化。
     * @param node 要插入的节点
     * @return 节点的前任
     */
    private Node enq(final Node node) {
        for (;;) { // 无线循环
            Node t = tail; // 拿到队列尾,第一次为null
            if (t == null) { // 如果为null，则需要初始化队列
                if (compareAndSetHead(new Node())) // 队列头放入空的node
                    tail = head; // 设置头结点，尾节点等于头结点
            } else {
                node.prev = t; // 当前节点前一个节点为t，第一次为空的node
                if (compareAndSetTail(t, node)) { // node作为尾节点替换t，t为头节点
                    t.next = node; // t的下个节点为node
                    return t; // 返回node的前节点t
                }
            }
        }
    }

    /**
     * 为当前线程和给定模式创建和入队节点。(排他锁是null，共享锁是new Node() )
     *
     * @param mode Node.EXCLUSIVE for exclusive（null）, Node.SHARED for shared（new Node()）
     * @return 新的node
     */
    private Node addWaiter(Node mode) { // 把节点添加到队列的尾部
        Node node = new Node(Thread.currentThread(), mode); // 传入当前线程和节点（null或new Node() ）
        // 试试enq的快速路径;失败时备份到完整的enq
        Node pred = tail; // pred节点为尾结点
        if (pred != null) { // 尾结点不为null，初始化过了
            node.prev = pred; // 节点的前一个节点为之前的尾结点
            if (compareAndSetTail(pred, node)) { // 把node的节点作为尾节点
                pred.next = node;// 调整关系
                return node; // 返回当前节点
            }
        }
        // 若未初始化或cas失败则进入enq() 初始化或入队（循环操作）
        enq(node);
        return node;
    }

    /**
     * 将队列的头部设置为节点，从而退出队列。仅通过acquire方法调用。
     * 为了进行GC和抑制不必要的信号和遍历，还会为空出未使用的字段。
     *
     * @param node the node
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 唤醒node的后继节点(如果存在的话)。
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * 如果状态为负(即(可能需要信号)试着发出信号。
         * 如果这个操作失败，或者通过等待线程改变状态，这是可以的。
         */
        int ws = node.waitStatus; // 拿到该节点的等待状态
        if (ws < 0) // 若该节点状态为负的，则改为0 无状态
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * 唤醒的线程被保存在后续节点中，它通常只是下一个节点。
         * 但是，如果取消或显然为空，则从tail向后遍历，以找到实际的未取消后继。
         */
        Node s = node.next; // 拿到node的下一个节点
        if (s == null || s.waitStatus > 0) { // 如果s为null，或者中断了
            s = null; // s设为null，提前GC
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t; // 从tail往前遍历，找到waitStatus <= 0的
        }
        if (s != null) // 若s不为0，则唤醒该线程
            LockSupport.unpark(s.thread);
    }

    /**
     * 释放共享模式的动作——信号后继者，并确保传播。
     * (注意:对于独占模式，释放相当于调用unpark继任人的头，如果它需要信号。)
     */
    private void doReleaseShared() {
        /*
         * 确保一个版本传播，即使有其他正在进行的获取/发布。这是按照通常的方式进行的，
         * 如果它需要信号的话，就会试图卸下head的后继者。
         * 但如果没有，则将状态设置为PROPAGATE，以确保在发布后传播仍将继续。
         * 此外，我们必须循环，以防在执行此操作时添加新节点。
         * 另外，与unpark继任人的其他用途不同，我们需要知道CAS是否复位失败，是否重新检查。
         */
        for (;;) { // 无限循环
            Node h = head; // 拿到头，初始化之后是new Node
            if (h != null && h != tail) { // 判断队列里面有等待线程
                int ws = h.waitStatus; // 拿到等待状态
                if (ws == Node.SIGNAL) { // 若状态为-1，导火线
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) // 替换当前状态为0
                        continue;             // 若失败继续循环
                    unparkSuccessor(h); // 调用唤醒后续线程
                } // 非导火线状态，若ws为0无状态，则替换为-3，唤醒后续线程
                else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // 若失败继续循环
            }
            if (h == head)                   // 换头循环
                break; // 如果改变，跳出循环
        }
    }

    /**
     * 设置队列的头部，并检查后续队列是否在共享模式下等待，如果是，则传播如果传播> 0或传播状态已设置。
     *
     * @param node the node
     * @param propagate 从获取共享锁的返回值
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // 下面将记录旧头进行检查
        setHead(node); // 设置node为头节点
        /*
         * 尝试信号下一个排队节点如果:
         *   传播由调用者指示,
         *     或被记录(作为h.waitStatus 之前或之后的等待状态)
         *     (注意:这将使用等待状态的信号检查，因为传播状态可能会转换为信号。)
         * and
         *   下一个节点在共享模式下等待，或者我们不知道，因为它看起来是空的
         *
         * 这两种检查的保守性可能会导致不必要的唤醒，但只有在有多个竞速获得/释放时才会这样，
         * 所以大多数人现在或很快就需要信号。
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
                (h = head) == null || h.waitStatus < 0) {
            Node s = node.next; // 当前节点的下一个节点
            if (s == null || s.isShared()) // 若为null或者是共享状态（nextWaiter == new Node()）
                doReleaseShared(); // 释放共享锁
        }
    }

    // 各种版本获取的实用程序

    /**
     * 取消正在进行的获取尝试。
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // 如果节点不存在则忽略
        if (node == null)
            return;

        node.thread = null; // 节点线程设为null

        // 跳过取消的前任
        Node pred = node.prev;
        while (pred.waitStatus > 0) // 前任的状态大于0的话，则跳过，无线循环
            node.prev = pred = pred.prev;

        // predNext是要unsplice的明显节点。下面的情况将失败，如果不是，
        // 在这种情况下，我们失去了竞争对另一个取消或信号，所以没有进一步的行动是必要的。
        Node predNext = pred.next;

        // 这里可以使用无条件写代替CAS。
        // 在这个原子步骤之后，其他节点可以跳过我们。
        // 在此之前，我们不受其他线程的干扰。
        node.waitStatus = Node.CANCELLED;

        // 如果是最后一个则移除
        if (node == tail && compareAndSetTail(node, pred)) { // 替换为前一个
            compareAndSetNext(pred, predNext, null); // 把pred下一个设置为null，node摆脱关系
        } else {
            // 如果继任者需要信号，尝试设置pred的下一个链接
            // 所以它会得到1。否则唤醒它繁殖。
            int ws;
            if (pred != head &&
                    ((ws = pred.waitStatus) == Node.SIGNAL ||
                            (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                    pred.thread != null) { // 剔除该线程
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node); // 如果没有前面的线程，则唤醒后面的节点
            }

            node.next = node; // help GC
        }
    }

    /**
     * 检查和更新未能获取的节点的状态。如果线程阻塞，返回true。
     * 这是所有采集回路的主信号控制。需要pred == node.prev。
     *
     * @param pred 节点的前任保持的状态
     * @param node the node
     * @return {@code true} 如果线程阻塞
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // 获取失败后阻塞住
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * 这个节点已经设置了请求释放信号的状态，所以它可以安全地阻塞。
             */
            return true; // 若前节点阻塞住，则该节点也阻塞
        if (ws > 0) {
            /*
             * 如果前任中断了或取消了，跳过
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * 等待状态必须为0或传播。请指示我们需要信号，但不要阻塞。调用者将需要重试，以确保它不能获得在阻塞之前。
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * 方便的方法中断当前线程。
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * 方便阻塞后再检查是否中断
     *
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * 获取方式多种多样，有排他/共享模式和控制模式。
     * 每一种都大同小异，但又令人讨厌地不同。
     * 由于异常机制(包括确保我们在tryAcquire抛出异常时取消)和其他控制的交互作用，
     * 只有很少的因素是可能的，至少不会对性能造成太大的损害。
     */

    /**
     * 已在队列中的线程以独占的不可中断模式获取。用于条件等待方法以及获取。
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) { // 自旋
                final Node p = node.predecessor(); // 拿到节点前任
                if (p == head && tryAcquire(arg)) { // 前任如果为head的话，则唤醒当前节点
                    setHead(node); // 成功唤醒，把当前节点作为头结点
                    p.next = null; // help GC
                    failed = false;
                    return interrupted; // 返回中断状态
                }
                if (shouldParkAfterFailedAcquire(p, node) && // 获取失败阻塞住
                        parkAndCheckInterrupt()) // 检查中断
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 排他可中断模式获取锁
     * @param arg the acquire argument
     */
    private void doAcquireInterruptibly(int arg)
            throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE); // 添加排它锁
        boolean failed = true;
        try {
            for (;;) { // 自旋
                final Node p = node.predecessor(); // 拿到前任
                if (p == head && tryAcquire(arg)) { // 若是头结点并且获取到锁
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) && // 获取失败阻塞
                        parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 排他时间模式获取锁。
     *
     * @param arg 获取参数
     * @param nanosTimeout 最大等待时间
     * @return {@code true} 如果获取到
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE); // 排它锁
        boolean failed = true;
        try {
            for (;;) { // 自旋
                final Node p = node.predecessor(); // 前任
                if (p == head && tryAcquire(arg)) { // 获取锁
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) // 判断是否超时
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) && // 获取失败
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
     * 在共享非阻断模式下获取
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED); // 共享锁添加到队列
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) { // 自旋
                final Node p = node.predecessor(); // 拿到前任
                if (p == head) { // 若是头结点
                    int r = tryAcquireShared(arg); // 试图获取锁
                    if (r >= 0) { // 若大于0，设置头和传播数
                        setHeadAndPropagate(node, r); // 设置头和传播数
                        p.next = null; // help GC
                        if (interrupted) // 若是中断状态则中断
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
     * 在共享中断模式下获取
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
            throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) { // 自旋
                final Node p = node.predecessor(); // 拿到前任
                if (p == head) { // 若前任为头，则获取锁
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
     * 在共享时间模式下获取
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
     * 尝试以独占模式获取。该方法应该查询对象的状态是否允许以独占模式获取它，如果允许，则应该获取它。
     *
     * <p>执行获取的线程总是调用此方法。如果此方法报告失败，则获取方法可能会对线程进行排队(如果它还没有排队)，
     * 直到通过其他线程的释放发出信号。这可以用来实现Lock.tryLock()方法。
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg 获取参数。这个值总是传递给一个获取方法的值，或者是在进入一个条件wait时保存的值。
     *            该值是未解释的，可以表示您喜欢的任何内容。
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
     * 试图将状态设置为以独占模式反映发布。
     *
     * <p>执行release的线程总是调用此方法。
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
     * 尝试以共享模式获取。该方法应该查询对象的状态是否允许在共享模式下获取它，如果允许，则应该获取它。
     *
     * <p>执行获取的线程总是调用此方法。如果此方法报告失败，则获取方法可能会对线程进行排队(如果它还没有排队)，
     * 直到通过其他线程的释放发出信号。
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return 失败为负值;如果共享模式下的获取成功，但是没有后续的共享模式获取可以成功，
     * 则为零;如果在共享模式下获取成功，
     * 并且随后的共享模式获取也可能成功，那么在这种情况下，后续的等待线程必须检查可用性。
     * (支持三种不同的返回值，使此方法可以用于仅在某些情况下才进行获取的上下文中。)成功之后，这个对象就获得了。
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
     * 尝试将状态设置为以共享模式反映发布。
     *
     * <p>执行release的线程总是调用此方法。
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
     * 如果仅针对当前(调用)线程保持同步，则返回true。此方法在每次调用未等待的AbstractQueuedSynchronizer时调用。
     * ConditionObject方法。(等待方法调用release(int)。)
     *
     * <p>默认实现将抛出UnsupportedOperationException。
     * 此方法仅在AbstractQueuedSynchronizer内部调用。如果没有使用条件，则不必定义条件对象方法。
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * 在排他模式中获取，忽略阻断。通过最后一次调用tryAcquire(int)实现，返回成功。
     * 否则线程在队列中，可能重复阻塞和唤醒，调用tryAcquire(int)直到成功。这个方法可以用来实现方法Lock.lock()。
     *
     * @param arg 获取的参数。这个值传递给tryAcquire(int)，否则是非阻断的可以代表你喜欢的人和事。
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

    /**
     * 以独占模式获取，如果中断则中止。首先检查中断状态，然后至少调用一次tryAcquire(int)，
     * 成功后返回。否则，线程将排队，可能会重复阻塞和取消阻塞，调用tryAcquire(int)，
     * 直到成功或线程被中断。这个方法可以用来实现Lock.lockInterruptibly()方法。
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
     * 尝试以独占模式获取，如果中断将中止，如果给定超时超时将失败。
     * 首先检查中断状态，然后至少调用一次tryAcquire(int)，成功后返回。
     * 否则，线程将排队，可能会重复阻塞和取消阻塞，调用tryAcquire(int)，
     * 直到成功或线程中断或超时结束。此方法可用于实现方法锁。TimeUnit tryLock(long)。
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
     * 以独占模式发布。如果tryRelease(int)返回true，
     * 则通过解除一个或多个线程的阻塞来实现。这个方法可以用来实现Lock.unlock()方法。
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
     * 忽略中断，以共享模式获取。首先调用至少一次tryacquiremred (int)，
     * 成功后返回。否则，线程将排队，可能会反复阻塞和取消阻塞，调用tryacquiremred (int)直到成功。
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
     * 以共享模式获取，如果中断将中止。首先检查中断状态，然后至少调用一次tryacquiremrered (int)，
     * 成功后返回。否则，线程将排队，可能会重复阻塞和取消阻塞，调用tryacquiremred (int)，
     * 直到成功或线程被中断。
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
     * 尝试以共享模式获取，如果中断将中止，如果给定超时超时将失败。首先检查中断状态，
     * 然后至少调用一次tryacquiremrered (int)，成功后返回。否则，线程将排队，
     * 可能会重复阻塞和取消阻塞，调用tryacquiremred (int)，直到成功或线程中断或超时结束。
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
     * 以共享模式发布。如果tryReleaseShared(int)返回true，则通过取消阻塞一个或多个线程来实现。
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
     * 查询是否有线程正在等待获取。注意，由于中断和超时导致的取消可能在任何时候发生，
     * 所以真正的返回并不保证任何其他线程将获得。
     *
     * <p>在此实现中，此操作以常数时间返回。
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * 查询是否有任何线程争用过此同步器;也就是说，如果一个获取方法曾经被阻塞。
     *
     * <p>在此实现中，此操作以常数时间返回。
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * 返回队列中的第一个(等待时间最长的)线程，如果当前没有线程排队，则返回null。
     *
     * <p>在此实现中，此操作通常以常数时间返回，但如果其他线程同时修改队列，则可能在争用时进行迭代。
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * fastpath失败时调用的getFirstQueuedThread版本
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * 第一个节点通常是head.next。尝试获取它的线程字段，确保一致的读取:如果线程字段为空或s。
         * prev不再是head，然后一些其他线程在读取之间并发地执行setHead。在进行遍历之前，我们尝试了两次。
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
                s.prev == head && (st = s.thread) != null) ||
                ((h = head) != null && (s = h.next) != null &&
                        s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head的下一个字段可能还没有设置，或者可能在setHead之后取消了设置。
         * 所以我们必须检查一下tail是不是第一个节点。如果没有，我们继续，从尾部到头部安全地找到第一，保证终止。
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
     * 如果给定线程当前正在排队，则返回true。
     *
     * <p>此实现遍历队列以确定给定线程的存在。
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
     * 如果第一个排队的线程(如果存在)以排他模式等待，则返回{@code true}。
     * 如果这个方法返回{@code true}，并且当前线程正在尝试以共享模式获取
     * (也就是说，这个方法是从{@link # tryacquiresred}调用的)，
     * 那么可以保证当前线程不是第一个排队的线程。仅在ReentrantReadWriteLock中用作启发式。
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
                (s = h.next)  != null &&
                !s.isShared()         &&
                s.thread != null;
    }

    /**
     * 查询是否有任何线程等待获取的时间比当前线程长。
     *
     * <p>此方法的调用相当于(但可能比):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * <p>注意，由于中断和超时导致的取消可能随时发生，
     * 所以真正的返回并不保证其他线程会在当前线程之前获得。
     * 同样，在此方法返回false后，由于队列是空的，另一个线程也可能赢得入队。
     *
     * <p>该方法被设计用于一个公平的同步器，以避免碰撞。
     * 这样一个同步器的tryAcquire(int)方法应该返回false，
     * 如果这个方法返回true(除非这是一个重入获取)，
     * 那么它的tryacquired (int)方法应该返回一个负值。
     * 例如，一个公平的，可重入的，独占模式同步器的tryAcquire方法可能是这样的:
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
     * 返回等待获取的线程数量的估计值。这个值只是一个估计值，因为当这个方法遍历内部数据结构时，
     * 线程的数量可能会动态变化。该方法用于监控系统状态，不用于同步控制。
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
     * 返回一个包含可能正在等待获取的线程的集合。
     * 因为在构造这个结果时，实际的线程集可能会动态变化，
     * 所以返回的集合只是一个最佳效果的估计。返回集合的元素没有特定的顺序。
     * 这种方法的目的是为了方便构建提供更广泛的监视设施的子类。
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
     * 返回一个集合，其中包含可能正在等待以独占模式获取的线程。
     * 它具有与getQueuedThreads()相同的属性，只是它只返回那些由于独占获取而等待的线程。
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
     * 返回一个集合，其中包含可能在共享模式下等待获取的线程。
     * 它具有与getQueuedThreads()相同的属性，只是它只返回那些由于共享获取而等待的线程。
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
     * 返回标识此同步器及其状态的字符串。方括号中的状态包括字符串“state =”，
     * 后跟getState()的当前值，以及“nonempty”或“empty”，这取决于队列是否为空。
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
     * 节点。prev可以是非空的，但还不能放在队列上，因为将它放在队列上的ca可能会失败。
     * 所以我们必须从尾部开始遍历以确保它确实成功了。在对这个方法的调用中，它总是在尾部附近，除非CAS失败(这是不太可能的)，否则它将在那里，因此我们几乎不会遍历太多。
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
     * 如果节点从尾部向后搜索同步队列，返回true。只在isOnSyncQueue需要时调用。
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
     * 将节点从条件队列转移到同步队列。如果成功返回true。
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     */
    final boolean transferForSignal(Node node) {
        /*
         * 如果无法更改等待状态，则节点已被取消。
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         *  拼接到队列，并尝试设置前辈的等待状态，以表明线程(可能)正在等待。
         * 如果取消或设置等待状态失败，则唤醒并重新同步(在这种情况下，等待状态可能暂时错误，但不会造成任何伤害)。
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * 传输节点，如果需要，在一个取消的等待后同步队列。如果线程在被发送信号之前被取消，则返回true。
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
         * 如果我们丢失了一个saignal()，那么我们就不能继续，直到它完成它的enq()。
         * 在不完全转移过程中取消是罕见的，也是短暂的，所以只要旋转就可以了。
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
            int savedState = getState(); // 当前锁状态
            if (release(savedState)) { // 释放当前锁，唤醒下一个线程
                failed = false;
                return savedState; // 保存state
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
     * 查询给定的条件对象是否使用此同步器作为其锁。
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * 查询是否有线程正在等待与此同步器关联的给定条件。
     * 注意，因为超时和中断可能随时发生，所以真正的返回并不保证将来的信号将唤醒任何线程。
     * 该方法主要用于监控系统状态。
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
     * 返回与此同步器关联的给定条件上等待的线程数的估计值。
     * 请注意，由于超时和中断可能随时发生，因此估计值仅作为实际等待者数量的上限。
     * 此方法设计用于监视系统状态，而不是用于同步控制。
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
     * 返回一个集合，其中包含可能正在等待与此同步器关联的给定条件的线程。
     * 因为在构造这个结果时，实际的线程集可能会动态变化，所以返回的集合只是一个最佳效果的估计。
     * 返回集合的元素没有特定的顺序。
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
     * 作为{@link锁}实现的基础的{@link AbstractQueuedSynchronizer}的条件实现。
     *
     * <p>这个类的方法文档描述机制，而不是从锁和条件用户的角度来描述行为规范。
     * 这个类的导出版本通常需要伴随着描述依赖于关联的{@code AbstractQueuedSynchronizer}的条件语义的文档。
     *
     * <p>这个类是可序列化的，但是所有字段都是瞬态的，因此反序列化的条件没有等待者。
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** 条件队列第一个节点 */
        private transient Node firstWaiter;
        /** 条件队列最后一个节点 */
        private transient Node lastWaiter;

        /**
         * 创建一个新的 {@code ConditionObject} 实例.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * 添加一个新的等待队列。
         * @return its new wait node
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // 如果最后一个等待取消了，清空
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
         * 删除和传输节点，直到命中不可取消的一个或null。
         * 从signal中分离出来，部分原因是为了鼓励编译器在没有等待器的情况下内联。
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
         * 删除和传输所有节点。
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
         * 从条件队列取消已取消的侍者节点的链接。只有在持有锁时才调用。
         * 这在条件等待期间发生取消时调用，在看到lastWaiter已被取消时插入新waiter时调用。
         * 需要使用此方法来避免在没有信号时的垃圾保留。因此，即使它可能需要完整的遍历，
         * 也只有在没有信号的情况下发生超时或取消时，它才会起作用。它遍历所有节点，
         * 而不是停在一个特定的目标上，取消指向垃圾节点的所有指针的链接，而不需要在取消风暴期间进行多次重新遍历。
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

        // 公共方法

        /**
         * 将最长等待线程(如果存在)从该条件的等待队列移动到拥有锁的等待队列。
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
         * 将此条件下的所有线程从等待队列移动到拥有锁的等待队列。
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
         * 实现不可中断条件等待。
         * <ol>
         * <li> 保存由{@link #getState}返回的锁状态。
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
         * 实现中断条件等待：
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
         * 实现定时条件等待。
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
         * 实现了绝对定时条件等待。
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
         * 实现定时条件等待。
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
         * 如果该条件是由给定的同步对象创建的，则返回true。
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * 查询是否有线程在等待此条件。实现了{@link AbstractQueuedSynchronizer # hasWaiters (ConditionObject)}。
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
         * 返回等待此条件的线程数的估计数。
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
         * 返回一个集合，其中包含可能正在等待此条件的线程。
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
     * 设置来支持比较和设置。我们需要在这里本地实现这一点:为了允许将来的增强，
     * 我们不能显式地子类化AtomicInteger，否则它将是有效和有用的。
     * 因此，我们使用hotspot intrinsics API本机实现，以减轻其带来的不便。
     * 当我们这样做时，我们对其他CASable字段做同样的事情(这可以用原子字段更新程序来完成)。
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
