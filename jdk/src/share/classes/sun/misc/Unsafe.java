/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.misc;

import java.security.*;
import java.lang.reflect.*;

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;


/**
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * @author John R. Rose
 * @see #getUnsafe
 */

public final class Unsafe {

    private static native void registerNatives();

    static {
        registerNatives();
        sun.reflect.Reflection.registerMethodsToFilter(Unsafe.class, "getUnsafe");
    }

    private Unsafe() {
    }

    private static final Unsafe theUnsafe = new Unsafe();

    //为调用者提供执行不安全操作的能力。
    // 返回的<code>不安全</code>对象应该由调用者小心保护，因为它可以用于在任意内存地址读写数据。
    // 它绝不能传递给不可信的代码。
    // 此类中的大多数方法都是非常低级的，并且对应于少量的硬件指令(在典型的机器上)。鼓励编译器相应地优化这些方法。
    // 下面是一个关于使用不安全操作的习语:

    /**
     * class MyTrustedClass {
     * private static final Unsafe unsafe = Unsafe.getUnsafe();
     * ...
     * private long myCountAddress = ...;
     * public int getCount() {
     * return unsafe.getByte(myCountAddress);
     * }
     * }
     **/
    @CallerSensitive // 此注解可以找到真正调用的类
    public static Unsafe getUnsafe() {
        Class<?> caller = Reflection.getCallerClass();
        if (!VM.isSystemDomainLoader(caller.getClassLoader()))
            throw new SecurityException("Unsafe");
        return theUnsafe;
    }

    // peek and poke operations
    // (编译器应该将这些优化为内存操作)
    // 它们处理Java堆中的对象字段。
    // 它们将不能处理压缩数组的元素。


    /* *
    * 从给定的Java变量获取一个值。
    * 更具体地说，在给定偏移量处从给定对象<code>o</code>，
    * 或者(如果<code>o</code>为null)从其数值为给定偏移量的内存地址中获取字段或数组元素。
    * 结果是未定义的，除非下列情况之一是真实的:
    * 偏移量是从{@link java.lang.reflect上的{@link #objectFieldOffset}获得的。
    * 某些Java字段的字段}和<code>o</code>引用的对象是与该字段的类兼容的类。
    * 偏移量和对象引用<code>o</code> (null或非null)都是通过{@link #staticFieldOffset}和
    * {@link #staticFieldBase}(分别)从某个Java字段的反射{@link Field}表示获得的。
    * 如果上述情况之一为真，则调用引用特定的Java变量(字段或数组元素)。
    * 但是，如果该变量实际上不是此方法返回的类型，则结果是未定义的。
    * 该方法通过两个参数引用一个变量，因此它(实际上)为Java变量提供了一个<em>双寄存器</em>寻址模式。
    * 当对象引用为空时，此方法使用其偏移量作为绝对地址。这在操作上类似于{@link #getInt(long)}等方法，
    * 它们(实际上)为非java变量提供了<em>单寄存器</em>寻址模式。
    * 但是，由于Java变量在内存中的布局可能与非Java变量不同，因此程序员不应该假设这两种寻址模式是等价的。
    * 此外，程序员应该记住，双寄存器寻址模式的偏移量不能与单寄存器寻址模式中使用的long混淆。
    * 如果上述情况之一为真，则调用引用特定的Java变量(字段或数组元素)。
    * 但是，如果该变量实际上不是此方法返回的类型，则结果是未定义的。
    * 该方法通过两个参数引用一个变量，以此类推它(实际上)为Java变量提供了一个<em>双寄存器</em>寻址模式。
    * 当对象引用为空时，此方法使用其偏移量作为绝对地址。这在操作上类似于{@link #getInt(long)}等方法，它们(实际上)为非java变量提供了<em>单寄存器</em>寻址模式。但是，由于Java变量在内存中的布局可能与非Java变量不同，因此程序员不应该假设这两种寻址模式是等价的。此外，程序员应该记住，双寄存器寻址模式的偏移量不能与单寄存器寻址模式中使用的long混淆。
    @ param o 变量所在的Java堆对象(如果有的话),否则为空
    @ parma offset 指出变量在Java堆对象中的位置(如果有的话)，否则就是静态定位变量的内存地址
    @ return 从指定的Java变量获取的值
    @ throws 未定义
    */
    public native int getInt(Object o, long offset);

    /**
     * Stores a value into a given Java variable.
     * <p>
     * 将一个值存储到给定的Java变量中。
     * <p>
     * 前两个参数被解释为{@link #getInt(Object, long)}引用特定的Java变量(字段或数组元素)。
     * 给定的值存储在该变量中。
     * <p>
     * 变量的类型必须与方法参数<code>x</code>相同。
     *
     * @param o      Java heap object in which the variable resides, if any, else
     *               null
     * @param offset indication of where the variable resides in a Java heap
     *               object, if any, else a memory address locating the variable
     *               statically
     * @param x      the value to store into the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *                          {@link NullPointerException}
     */
    public native void putInt(Object o, long offset, int x);

    /**
     * 从给定的Java变量中获取一个引用值。
     */
    public native Object getObject(Object o, long offset);

    /**
     * 将引用值存储到给定的Java变量中。
     * <p>
     * 除非存储的引用<code>x</code>是null或匹配字段类型，否则结果是未定义的。
     *
     * @see #putInt(Object, int, int)
     */
    public native void putObject(Object o, long offset, Object x);

    /**
     * @see #getInt(Object, long)
     */
    public native boolean getBoolean(Object o, long offset);

    /**
     * @see #putInt(Object, int, int)
     */
    public native void putBoolean(Object o, long offset, boolean x);

    /**
     * @see #getInt(Object, long)
     */
    public native byte getByte(Object o, long offset);

    /**
     * @see #putInt(Object, int, int)
     */
    public native void putByte(Object o, long offset, byte x);

    /**
     * @see #getInt(Object, long)
     */
    public native short getShort(Object o, long offset);

    /**
     * @see #putInt(Object, int, int)
     */
    public native void putShort(Object o, long offset, short x);

    /**
     * @see #getInt(Object, long)
     */
    public native char getChar(Object o, long offset);

    /**
     * @see #putInt(Object, int, int)
     */
    public native void putChar(Object o, long offset, char x);

    /**
     * @see #getInt(Object, long)
     */
    public native long getLong(Object o, long offset);

    /**
     * @see #putInt(Object, int, int)
     */
    public native void putLong(Object o, long offset, long x);

    /**
     * @see #getInt(Object, long)
     */
    public native float getFloat(Object o, long offset);

    /**
     * @see #putInt(Object, int, int)
     */
    public native void putFloat(Object o, long offset, float x);

    /**
     * @see #getInt(Object, long)
     */
    public native double getDouble(Object o, long offset);

    /**
     * @see #putInt(Object, int, int)
     */
    public native void putDouble(Object o, long offset, double x);

    /**
     * 与其他具有32位偏移量的方法一样，这个方法在以前的版本中是原生的，但是现在是一个包装器，它只是将偏移量转换为一个长值。
     * 它提供了与针对1.4编译的字节码的向后兼容性。
     */
    @Deprecated
    public int getInt(Object o, int offset) {
        return getInt(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putInt(Object o, int offset, int x) {
        putInt(o, (long) offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public Object getObject(Object o, int offset) {
        return getObject(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putObject(Object o, int offset, Object x) {
        putObject(o, (long) offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public boolean getBoolean(Object o, int offset) {
        return getBoolean(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putBoolean(Object o, int offset, boolean x) {
        putBoolean(o, (long) offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public byte getByte(Object o, int offset) {
        return getByte(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putByte(Object o, int offset, byte x) {
        putByte(o, (long) offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public short getShort(Object o, int offset) {
        return getShort(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putShort(Object o, int offset, short x) {
        putShort(o, (long) offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public char getChar(Object o, int offset) {
        return getChar(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putChar(Object o, int offset, char x) {
        putChar(o, (long) offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public long getLong(Object o, int offset) {
        return getLong(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putLong(Object o, int offset, long x) {
        putLong(o, (long) offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public float getFloat(Object o, int offset) {
        return getFloat(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putFloat(Object o, int offset, float x) {
        putFloat(o, (long) offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public double getDouble(Object o, int offset) {
        return getDouble(o, (long) offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putDouble(Object o, int offset, double x) {
        putDouble(o, (long) offset, x);
    }

    // 它们处理C堆中的值。

    // 从给定的内存地址获取值。如果地址为0，或者没有指向从{@link #allocateMemory}获得的块，则结果是未定义的。
    public native byte getByte(long address);

    // 将值存储到给定的内存地址中。如果地址为0，或者没有指向从{@link #allocateMemory}获得的块，则结果是未定义的。
    public native void putByte(long address, byte x);

    /**
     * @see #getByte(long)
     */
    public native short getShort(long address);

    /**
     * @see #putByte(long, byte)
     */
    public native void putShort(long address, short x);

    /**
     * @see #getByte(long)
     */
    public native char getChar(long address);

    /**
     * @see #putByte(long, byte)
     */
    public native void putChar(long address, char x);

    /**
     * @see #getByte(long)
     */
    public native int getInt(long address);

    /**
     * @see #putByte(long, byte)
     */
    public native void putInt(long address, int x);

    /**
     * @see #getByte(long)
     */
    public native long getLong(long address);

    /**
     * @see #putByte(long, byte)
     */
    public native void putLong(long address, long x);

    /**
     * @see #getByte(long)
     */
    public native float getFloat(long address);

    /**
     * @see #putByte(long, byte)
     */
    public native void putFloat(long address, float x);

    /**
     * @see #getByte(long)
     */
    public native double getDouble(long address);

    /**
     * @see #putByte(long, byte)
     */
    public native void putDouble(long address, double x);

    // 从给定的内存地址获取本机指针。如果地址为0，
    // 或者没有指向从{@link #allocateMemory}获得的块，则结果是未定义的。
    public native long getAddress(long address);

    // 如果本机指针的宽度小于64位，则它将作为无符号数扩展为Java长。
    // 指针可以通过任何给定的字节偏移量建立索引，只需将偏移量(作为一个简单的整数)添加到表示指针的long中即可。
    // 从目标地址实际读取的字节数可以通过查询{@link #addressSize}确定。
    public native void putAddress(long address, long x);

    /// 分配内存的替换，分配空内存

    // 分配一个新的本机内存块，其大小以字节为单位。内存内容未初始化;它们通常是垃圾。生成的本机指针永远不会为零，并且将对所有值类型进行对齐。通过调用{@link #freeMemory}来处理这个内存，或者使用{@link #reallocateMemory}调整它的大小。
// @throws 如果大小为负或者对于本机size_t类型来说太大，则为IllegalArgumentException
// 如果分配被系统拒绝，则为OutOfMemoryError
    public native long allocateMemory(long bytes);

    // 调整本机内存的新块的大小，使之达到给定的大小(以字节为单位)。
    // 超过旧块大小的新块的内容未初始化;它们通常是垃圾。当且仅当请求的大小为0时，生成的本机指针将为0。
    // 生成的本机指针将对所有值类型进行对齐。通过调用{@link #freeMemory}来处理这个内存，
    // 或者使用{@link #reallocateMemory}调整它的大小。
    // 传递给此方法的地址可能为null，在这种情况下将执行分配。
    public native long reallocateMemory(long address, long bytes);

    // 将给定内存块中的所有字节设置为一个固定值(通常为零)。
// 该方法通过两个参数确定块的基本地址，因此它提供了一个<em>双寄存器</em>寻址模式，
// 如{@link #getInt(Object,long)}中所讨论的。当对象引用为空时，偏移量提供一个绝对基址。
// 存储单元是由地址和长度参数决定大小的一致(原子)单元。如果有效地址和长度都是8的模，
// 那么存储将以“long”单元进行。
// 如果有效地址和长度是(resp.)偶数模4或2，则存储以“int”或“short”为单位。
    public native void setMemory(Object o, long offset, long bytes, byte value);

    // 将给定内存块中的所有字节设置为一个固定值(通常为零)。这提供了一个<em>单寄存器</em>寻址模式，
    // 如{@link #getInt(Object,long)}中所讨论的。
// 相当于<code>setMemory(null, address, bytes, value)</code>。
    public void setMemory(long address, long bytes, byte value) {
        setMemory(null, address, bytes, value);
    }

    // 将给定内存块中的所有字节设置为另一个块的副本。
// 这个方法通过两个参数来确定每个块的基本地址，因此它提供了一个<em>双寄存器</em>寻址模式，
// 正如{@link #getInt(Object,long)}中所讨论的那样。当对象引用为空时，偏移量提供一个绝对基址。
// 传输是由地址和长度参数决定大小的相干(原子)单位。如果有效地址和长度都是偶数模8，那么传输将以“long”单位进行。
// 如果有效地址和长度是(resp.)偶数模4或2，则传输以“int”或“short”为单位。
    public native void copyMemory(Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes);

    // 将给定内存块中的所有字节设置为另一个块的副本。这提供了一个<em>单寄存器</em>寻址模式，
    // 如{@link #getInt(Object,long)}中所讨论的。
// 相当于<code>copyMemory(null, srcAddress, null, destAddress, bytes)</code>。
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        copyMemory(null, srcAddress, null, destAddress, bytes);
    }

    // 从{@link #allocateMemory}或{@link #reallocateMemory}获得的本机内存块的配置。
    // 传递给此方法的地址可能为null，在这种情况下不采取任何操作。
    public native void freeMemory(long address);

// 随机访问


    public static final int INVALID_FIELD_OFFSET = -1;

    /**
     * @deprecated 从1.4.1开始，静态字段使用{@link #staticFieldOffset}，
     * 非静态字段使用{@link #objectFieldOffset}。
     */
    @Deprecated
    public int fieldOffset(Field f) {
        if (Modifier.isStatic(f.getModifiers()))
            return (int) staticFieldOffset(f);
        else
            return (int) objectFieldOffset(f);
    }

    /**
     * @deprecated 返回用于访问给定类中的某些静态字段的基本地址。
     * <p>
     * 从1.4.1开始，使用{@link #staticFieldBase(Field)}获得属于特定{@link字段}的基。此方法仅适用于将给定类的所有静态信息存储在一个位置的jvm。
     */
    @Deprecated
    public Object staticFieldBase(Class<?> c) {
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers())) {
                return staticFieldBase(fields[i]);
            }
        }
        return null;
    }

    // 报告给定字段在其类的存储分配中的位置。不要期望在这个偏移量上执行任何运算;
    // 它只是一个被传递给不安全的堆内存访问器的cookie。
// 任何给定的字段都将始终具有相同的偏移量和基，并且相同类的两个不同字段都不会具有相同的偏移量和基。
// 从1.4.1开始，字段的偏移量被表示为long值，尽管Sun JVM没有使用最重要的32位。
// 但是，将静态字段存储在绝对地址的JVM实现可以使用长偏移量和空基指针来以{@link #getInt(Object,long)}可用的形式表示字段位置。
// 因此，要在64位平台上移植到此类jvm的代码必须保留所有静态字段偏移量。
    public native long staticFieldOffset(Field f);

    // 与{@link #staticFieldBase}一起报告给定静态字段的位置。
// 不要期望在这个偏移量上执行任何运算;它只是一个被传递给不安全的堆内存访问器的cookie。
// 任何给定的字段都会有相同的偏移量，并且同一个类的两个不同的字段都不会有相同的偏移量。
    public native long objectFieldOffset(Field f);

    // 从1.4.1开始，字段的偏移量被表示为long值，尽管Sun JVM没有使用最重要的32位。
    // 很难想象JVM技术需要在非数组对象中编码一个偏移量，但是，为了与这个类中的其他方法保持一致，
    // 这个方法将结果报告为一个long值。
// 获取基“对象”(如果有的话)，使用它可以通过{@link #getInt(Object, long)}之类的方法访问给定类的静态字段。
// 此值可以为空。这个值可以引用一个对象，
// 它是一个“cookie”，不能保证它是一个真正的对象，它不应该以任何方式使用，除了作为这个类中的get和put例程的参数。
    public native Object staticFieldBase(Field f);

    // 检测给定的类是否需要初始化。这通常与获取类的静态字段基一起使用。
    public native boolean shouldBeInitialized(Class<?> c);

    // 确保已初始化给定的类。这通常与获取类的静态字段基一起使用。
    public native void ensureClassInitialized(Class<?> c);

    // 报告给定数组类的存储分配中第一个元素的偏移量。如果{@link #arrayIndexScale}为同一个类返回一个非零值，
    // 则可以使用该比例因子和这个基偏移量来形成新的偏移量，以访问给定类的数组元素。
    public native int arrayBaseOffset(Class<?> arrayClass);

    /**
     * The value of {@code arrayBaseOffset(boolean[].class)}
     */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(boolean[].class);

    /**
     * The value of {@code arrayBaseOffset(byte[].class)}
     */
    public static final int ARRAY_BYTE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(byte[].class);

    /**
     * The value of {@code arrayBaseOffset(short[].class)}
     */
    public static final int ARRAY_SHORT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(short[].class);

    /**
     * The value of {@code arrayBaseOffset(char[].class)}
     */
    public static final int ARRAY_CHAR_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(char[].class);

    /**
     * The value of {@code arrayBaseOffset(int[].class)}
     */
    public static final int ARRAY_INT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(int[].class);

    /**
     * The value of {@code arrayBaseOffset(long[].class)}
     */
    public static final int ARRAY_LONG_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(long[].class);

    /**
     * The value of {@code arrayBaseOffset(float[].class)}
     */
    public static final int ARRAY_FLOAT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(float[].class);

    /**
     * The value of {@code arrayBaseOffset(double[].class)}
     */
    public static final int ARRAY_DOUBLE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(double[].class);

    /**
     * The value of {@code arrayBaseOffset(Object[].class)}
     */
    public static final int ARRAY_OBJECT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(Object[].class);

    /**
     * Report the scale factor for addressing elements in the storage
     * allocation of a given array class.  However, arrays of "narrow" types
     * will generally not work properly with accessors like {@link
     * #getByte(Object, int)}, so the scale factor for such classes is reported
     * as zero.
     *
     * @see #arrayBaseOffset
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    public native int arrayIndexScale(Class<?> arrayClass);

    /**
     * The value of {@code arrayIndexScale(boolean[].class)}
     */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE
            = theUnsafe.arrayIndexScale(boolean[].class);

    /**
     * The value of {@code arrayIndexScale(byte[].class)}
     */
    public static final int ARRAY_BYTE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(byte[].class);

    /**
     * The value of {@code arrayIndexScale(short[].class)}
     */
    public static final int ARRAY_SHORT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(short[].class);

    /**
     * The value of {@code arrayIndexScale(char[].class)}
     */
    public static final int ARRAY_CHAR_INDEX_SCALE
            = theUnsafe.arrayIndexScale(char[].class);

    /**
     * The value of {@code arrayIndexScale(int[].class)}
     */
    public static final int ARRAY_INT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(int[].class);

    /**
     * The value of {@code arrayIndexScale(long[].class)}
     */
    public static final int ARRAY_LONG_INDEX_SCALE
            = theUnsafe.arrayIndexScale(long[].class);

    /**
     * The value of {@code arrayIndexScale(float[].class)}
     */
    public static final int ARRAY_FLOAT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(float[].class);

    /**
     * The value of {@code arrayIndexScale(double[].class)}
     */
    public static final int ARRAY_DOUBLE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(double[].class);

    /**
     * The value of {@code arrayIndexScale(Object[].class)}
     */
    public static final int ARRAY_OBJECT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(Object[].class);

    // 报告通过{@link #putAddress}存储的本机指针的大小(以字节为单位)。这个值将是4或8。
    // 注意，其他基本类型的大小(存储在本机内存块中)完全由它们的信息内容决定。
    public native int addressSize();

    /**
     * The value of {@code addressSize()}
     */
    public static final int ADDRESS_SIZE = theUnsafe.addressSize();

    // 以字节为单位报告本机内存页的大小(不管它是什么)。这个值总是2的幂。
    public native int pageSize();


    /// random trusted operations from JNI:

    // 告诉VM定义一个类，不需要进行安全检查。默认情况下，类装入器和保护域来自调用者的类。
    public native Class<?> defineClass(String name, byte[] b, int off, int len,
                                       ClassLoader loader,
                                       ProtectionDomain protectionDomain);

// 定义一个类，但不要让类装入器或系统字典知道它。
// 对于每个CP条目，对应的CP补丁必须是null，或者有与它的标签匹配的a格式:
    public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);


    // 分配一个实例，但不运行任何构造函数。如果还没有类，则初始化该类
    public native Object allocateInstance(Class<?> cls)
            throws InstantiationException;

    /** @deprecated
    锁定对象。它必须通过{@link #monitorExit}解锁。*/
    public native void monitorEnter(Object o);

    /** @deprecated
    释放对象。它必须通过{@link #monitorEnter}被锁定。*/
    public native void monitorExit(Object o);

    /** @deprecated
    试图锁定对象。返回true或false以指示锁是否成功。如果是，则必须通过{@link #monitorExit}解锁该对象。*/
    public native boolean tryMonitorEnter(Object o);

    // 在不通知验证者的情况下抛出异常
    public native void throwException(Throwable ee);

// 比较与替换操作
    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     *
     * @return <tt>true</tt> if successful
     */
    public final native boolean compareAndSwapObject(Object o, long offset,
                                                     Object expected,
                                                     Object x);

    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     *
     * @return <tt>true</tt> if successful
     */
    public final native boolean compareAndSwapInt(Object o, long offset,
                                                  int expected,
                                                  int x);

    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     *
     * @return <tt>true</tt> if successful
     */
    public final native boolean compareAndSwapLong(Object o, long offset,
                                                   long expected,
                                                   long x);

    /**
     * Fetches a reference value from a given Java variable, with volatile
     * load semantics. Otherwise identical to {@link #getObject(Object, long)}
     */
    public native Object getObjectVolatile(Object o, long offset);

    /**
     * Stores a reference value into a given Java variable, with
     * volatile store semantics. Otherwise identical to {@link #putObject(Object, long, Object)}
     */
    public native void putObjectVolatile(Object o, long offset, Object x);

    /**
     * Volatile version of {@link #getInt(Object, long)}
     */
    public native int getIntVolatile(Object o, long offset);

    /**
     * Volatile version of {@link #putInt(Object, long, int)}
     */
    public native void putIntVolatile(Object o, long offset, int x);

    /**
     * Volatile version of {@link #getBoolean(Object, long)}
     */
    public native boolean getBooleanVolatile(Object o, long offset);

    /**
     * Volatile version of {@link #putBoolean(Object, long, boolean)}
     */
    public native void putBooleanVolatile(Object o, long offset, boolean x);

    /**
     * Volatile version of {@link #getByte(Object, long)}
     */
    public native byte getByteVolatile(Object o, long offset);

    /**
     * Volatile version of {@link #putByte(Object, long, byte)}
     */
    public native void putByteVolatile(Object o, long offset, byte x);

    /**
     * Volatile version of {@link #getShort(Object, long)}
     */
    public native short getShortVolatile(Object o, long offset);

    /**
     * Volatile version of {@link #putShort(Object, long, short)}
     */
    public native void putShortVolatile(Object o, long offset, short x);

    /**
     * Volatile version of {@link #getChar(Object, long)}
     */
    public native char getCharVolatile(Object o, long offset);

    /**
     * Volatile version of {@link #putChar(Object, long, char)}
     */
    public native void putCharVolatile(Object o, long offset, char x);

    /**
     * Volatile version of {@link #getLong(Object, long)}
     */
    public native long getLongVolatile(Object o, long offset);

    /**
     * Volatile version of {@link #putLong(Object, long, long)}
     */
    public native void putLongVolatile(Object o, long offset, long x);

    /**
     * Volatile version of {@link #getFloat(Object, long)}
     */
    public native float getFloatVolatile(Object o, long offset);

    /**
     * Volatile version of {@link #putFloat(Object, long, float)}
     */
    public native void putFloatVolatile(Object o, long offset, float x);

    /**
     * Volatile version of {@link #getDouble(Object, long)}
     */
    public native double getDoubleVolatile(Object o, long offset);

    /**
     * Volatile version of {@link #putDouble(Object, long, double)}
     */
    public native void putDoubleVolatile(Object o, long offset, double x);

    /**
     * Version of {@link #putObjectVolatile(Object, long, Object)}
     * that does not guarantee immediate visibility of the store to
     * other threads. This method is generally only useful if the
     * underlying field is a Java volatile (or if an array cell, one
     * that is otherwise only accessed using volatile accesses).
     */
    public native void putOrderedObject(Object o, long offset, Object x);

    /**
     * Ordered/Lazy version of {@link #putIntVolatile(Object, long, int)}
     */
    public native void putOrderedInt(Object o, long offset, int x);

    /**
     * Ordered/Lazy version of {@link #putLongVolatile(Object, long, long)}
     */
    public native void putOrderedLong(Object o, long offset, long x);

    // 取消阻塞在<tt>park</tt>上阻塞的给定线程，或者，如果没有阻塞，则导致随后对<tt>park</tt>不阻塞的调用。注意:这个操作是“不安全的”，仅仅因为调用者必须以某种方式确保线程没有被破坏。
    // 当从Java调用时(其中通常有对线程的活引用)，通常不需要任何特殊的东西来确保这一点，但是当从本机代码调用时，这不是几乎自动的。
    public native void unpark(Object thread);

    // 阻塞当前线程,返回时平衡< tt > unpark < / tt >发生,或平衡< tt > unpark < / tt >已经发生,或线程被中断,或者,如果不是绝对和时间不为零,
    // 给定时间纳秒运行,或者如果绝对,给定的期限以毫秒为单位自时代已经过去,或貌似(即。，没有“理由”地回来)。注意:这个操作是在不安全类中，因为<tt>unpark</tt>是，所以把它放在其他地方会很奇怪。
    public native void park(boolean isAbsolute, long time);

    // 获取分配给可用处理器的系统运行队列中各时间段的平均负载。该方法检索给定的<tt>nelem</tt>样本，并分配给给定的<tt>loadavg</tt>数组的元素。系统最多施加3个样本，分别代表过去1分钟、5分钟和15分钟的平均值。
// @param loadavg loadavg是一个双倍大小的错误数组
// @param nelems 返回的样本数量必须是1到3。
// @return 实际检索到的样本数量;如果无法获得负载平均值，则为-1。
    public native int getLoadAverage(double[] loadavg, int nelems);

// 下面包含基于实例的Java实现
// 不支持本机指令的平台

    // 在给定的代码偏移量时，原子地将给定的值添加到给定对象<code>o</code> >中的字段或数组元素的当前值。
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

    // 在给定的代码>偏移量</code>时，原子地将给定的值添加到给定对象<code>o</code> >中的字段或数组元素的当前值。
    public final long getAndAddLong(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }

    // 在给定的代码>偏移量</code>处，原子地将给定的值与给定对象<code>o</code> >中的字段或数组元素的当前值进行交换。
    public final int getAndSetInt(Object o, long offset, int newValue) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, newValue));
        return v;
    }

    // 在给定的代码>偏移量</code>处，原子地将给定的值与给定对象<code>o</code> >中的字段或数组元素的当前值进行交换。
    public final long getAndSetLong(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, newValue));
        return v;
    }

    // 在给定的代码>偏移量</code>处，
    // 原子地将给定的参考值与给定对象<code>o</code> >中的字段或数组元素的当前参考值进行交换。
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        Object v;
        do {
            v = getObjectVolatile(o, offset);
        } while (!compareAndSwapObject(o, offset, v, newValue));
        return v;
    }


    /**
     * Ensures lack of reordering of loads before the fence
     * with loads or stores after the fence.
     *
     * @since 1.8
     */
    public native void loadFence();

    /**
     * Ensures lack of reordering of stores before the fence
     * with loads or stores after the fence.
     *
     * @since 1.8
     */
    public native void storeFence();

    /**
     * Ensures lack of reordering of loads or stores before the fence
     * with loads or stores after the fence.
     *
     * @since 1.8
     */
    public native void fullFence();

    /**
     * Throws IllegalAccessError; for use by the VM.
     *
     * @since 1.8
     */
    private static void throwIllegalAccessError() {
        throw new IllegalAccessError();
    }

}
