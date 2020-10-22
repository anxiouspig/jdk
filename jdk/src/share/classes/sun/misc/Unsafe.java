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

    //Ϊ�������ṩִ�в���ȫ������������
    // ���ص�<code>����ȫ</code>����Ӧ���ɵ�����С�ı�������Ϊ�����������������ڴ��ַ��д���ݡ�
    // �������ܴ��ݸ������ŵĴ��롣
    // �����еĴ�����������Ƿǳ��ͼ��ģ����Ҷ�Ӧ��������Ӳ��ָ��(�ڵ��͵Ļ�����)��������������Ӧ���Ż���Щ������
    // ������һ������ʹ�ò���ȫ������ϰ��:

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
    @CallerSensitive // ��ע������ҵ��������õ���
    public static Unsafe getUnsafe() {
        Class<?> caller = Reflection.getCallerClass();
        if (!VM.isSystemDomainLoader(caller.getClassLoader()))
            throw new SecurityException("Unsafe");
        return theUnsafe;
    }

    // peek and poke operations
    // (������Ӧ�ý���Щ�Ż�Ϊ�ڴ����)
    // ���Ǵ���Java���еĶ����ֶΡ�
    // ���ǽ����ܴ���ѹ�������Ԫ�ء�


    /* *
    * �Ӹ�����Java������ȡһ��ֵ��
    * �������˵���ڸ���ƫ�������Ӹ�������<code>o</code>��
    * ����(���<code>o</code>Ϊnull)������ֵΪ����ƫ�������ڴ��ַ�л�ȡ�ֶλ�����Ԫ�ء�
    * �����δ����ģ������������֮һ����ʵ��:
    * ƫ�����Ǵ�{@link java.lang.reflect�ϵ�{@link #objectFieldOffset}��õġ�
    * ĳЩJava�ֶε��ֶ�}��<code>o</code>���õĶ���������ֶε�����ݵ��ࡣ
    * ƫ�����Ͷ�������<code>o</code> (null���null)����ͨ��{@link #staticFieldOffset}��
    * {@link #staticFieldBase}(�ֱ�)��ĳ��Java�ֶεķ���{@link Field}��ʾ��õġ�
    * ����������֮һΪ�棬����������ض���Java����(�ֶλ�����Ԫ��)��
    * ���ǣ�����ñ���ʵ���ϲ��Ǵ˷������ص����ͣ�������δ����ġ�
    * �÷���ͨ��������������һ�������������(ʵ����)ΪJava�����ṩ��һ��<em>˫�Ĵ���</em>Ѱַģʽ��
    * ����������Ϊ��ʱ���˷���ʹ����ƫ������Ϊ���Ե�ַ�����ڲ�����������{@link #getInt(long)}�ȷ�����
    * ����(ʵ����)Ϊ��java�����ṩ��<em>���Ĵ���</em>Ѱַģʽ��
    * ���ǣ�����Java�������ڴ��еĲ��ֿ������Java������ͬ����˳���Ա��Ӧ�ü���������Ѱַģʽ�ǵȼ۵ġ�
    * ���⣬����ԱӦ�ü�ס��˫�Ĵ���Ѱַģʽ��ƫ���������뵥�Ĵ���Ѱַģʽ��ʹ�õ�long������
    * ����������֮һΪ�棬����������ض���Java����(�ֶλ�����Ԫ��)��
    * ���ǣ�����ñ���ʵ���ϲ��Ǵ˷������ص����ͣ�������δ����ġ�
    * �÷���ͨ��������������һ���������Դ�������(ʵ����)ΪJava�����ṩ��һ��<em>˫�Ĵ���</em>Ѱַģʽ��
    * ����������Ϊ��ʱ���˷���ʹ����ƫ������Ϊ���Ե�ַ�����ڲ�����������{@link #getInt(long)}�ȷ���������(ʵ����)Ϊ��java�����ṩ��<em>���Ĵ���</em>Ѱַģʽ�����ǣ�����Java�������ڴ��еĲ��ֿ������Java������ͬ����˳���Ա��Ӧ�ü���������Ѱַģʽ�ǵȼ۵ġ����⣬����ԱӦ�ü�ס��˫�Ĵ���Ѱַģʽ��ƫ���������뵥�Ĵ���Ѱַģʽ��ʹ�õ�long������
    @ param o �������ڵ�Java�Ѷ���(����еĻ�),����Ϊ��
    @ parma offset ָ��������Java�Ѷ����е�λ��(����еĻ�)��������Ǿ�̬��λ�������ڴ��ַ
    @ return ��ָ����Java������ȡ��ֵ
    @ throws δ����
    */
    public native int getInt(Object o, long offset);

    /**
     * Stores a value into a given Java variable.
     * <p>
     * ��һ��ֵ�洢��������Java�����С�
     * <p>
     * ǰ��������������Ϊ{@link #getInt(Object, long)}�����ض���Java����(�ֶλ�����Ԫ��)��
     * ������ֵ�洢�ڸñ����С�
     * <p>
     * ���������ͱ����뷽������<code>x</code>��ͬ��
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
     * �Ӹ�����Java�����л�ȡһ������ֵ��
     */
    public native Object getObject(Object o, long offset);

    /**
     * ������ֵ�洢��������Java�����С�
     * <p>
     * ���Ǵ洢������<code>x</code>��null��ƥ���ֶ����ͣ���������δ����ġ�
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
     * ����������32λƫ�����ķ���һ���������������ǰ�İ汾����ԭ���ģ�����������һ����װ������ֻ�ǽ�ƫ����ת��Ϊһ����ֵ��
     * ���ṩ�������1.4������ֽ�����������ԡ�
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

    // ���Ǵ���C���е�ֵ��

    // �Ӹ������ڴ��ַ��ȡֵ�������ַΪ0������û��ָ���{@link #allocateMemory}��õĿ飬������δ����ġ�
    public native byte getByte(long address);

    // ��ֵ�洢���������ڴ��ַ�С������ַΪ0������û��ָ���{@link #allocateMemory}��õĿ飬������δ����ġ�
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

    // �Ӹ������ڴ��ַ��ȡ����ָ�롣�����ַΪ0��
    // ����û��ָ���{@link #allocateMemory}��õĿ飬������δ����ġ�
    public native long getAddress(long address);

    // �������ָ��Ŀ��С��64λ����������Ϊ�޷�������չΪJava����
    // ָ�����ͨ���κθ������ֽ�ƫ��������������ֻ�轫ƫ����(��Ϊһ���򵥵�����)��ӵ���ʾָ���long�м��ɡ�
    // ��Ŀ���ַʵ�ʶ�ȡ���ֽ�������ͨ����ѯ{@link #addressSize}ȷ����
    public native void putAddress(long address, long x);

    /// �����ڴ���滻��������ڴ�

    // ����һ���µı����ڴ�飬���С���ֽ�Ϊ��λ���ڴ�����δ��ʼ��;����ͨ�������������ɵı���ָ����Զ����Ϊ�㣬���ҽ�������ֵ���ͽ��ж��롣ͨ������{@link #freeMemory}����������ڴ棬����ʹ��{@link #reallocateMemory}�������Ĵ�С��
// @throws �����СΪ�����߶��ڱ���size_t������˵̫����ΪIllegalArgumentException
// ������䱻ϵͳ�ܾ�����ΪOutOfMemoryError
    public native long allocateMemory(long bytes);

    // ���������ڴ���¿�Ĵ�С��ʹ֮�ﵽ�����Ĵ�С(���ֽ�Ϊ��λ)��
    // �����ɿ��С���¿������δ��ʼ��;����ͨ�������������ҽ�������Ĵ�СΪ0ʱ�����ɵı���ָ�뽫Ϊ0��
    // ���ɵı���ָ�뽫������ֵ���ͽ��ж��롣ͨ������{@link #freeMemory}����������ڴ棬
    // ����ʹ��{@link #reallocateMemory}�������Ĵ�С��
    // ���ݸ��˷����ĵ�ַ����Ϊnull������������½�ִ�з��䡣
    public native long reallocateMemory(long address, long bytes);

    // �������ڴ���е������ֽ�����Ϊһ���̶�ֵ(ͨ��Ϊ��)��
// �÷���ͨ����������ȷ����Ļ�����ַ��������ṩ��һ��<em>˫�Ĵ���</em>Ѱַģʽ��
// ��{@link #getInt(Object,long)}�������۵ġ�����������Ϊ��ʱ��ƫ�����ṩһ�����Ի�ַ��
// �洢��Ԫ���ɵ�ַ�ͳ��Ȳ���������С��һ��(ԭ��)��Ԫ�������Ч��ַ�ͳ��ȶ���8��ģ��
// ��ô�洢���ԡ�long����Ԫ���С�
// �����Ч��ַ�ͳ�����(resp.)ż��ģ4��2����洢�ԡ�int����short��Ϊ��λ��
    public native void setMemory(Object o, long offset, long bytes, byte value);

    // �������ڴ���е������ֽ�����Ϊһ���̶�ֵ(ͨ��Ϊ��)�����ṩ��һ��<em>���Ĵ���</em>Ѱַģʽ��
    // ��{@link #getInt(Object,long)}�������۵ġ�
// �൱��<code>setMemory(null, address, bytes, value)</code>��
    public void setMemory(long address, long bytes, byte value) {
        setMemory(null, address, bytes, value);
    }

    // �������ڴ���е������ֽ�����Ϊ��һ����ĸ�����
// �������ͨ������������ȷ��ÿ����Ļ�����ַ��������ṩ��һ��<em>˫�Ĵ���</em>Ѱַģʽ��
// ����{@link #getInt(Object,long)}�������۵�����������������Ϊ��ʱ��ƫ�����ṩһ�����Ի�ַ��
// �������ɵ�ַ�ͳ��Ȳ���������С�����(ԭ��)��λ�������Ч��ַ�ͳ��ȶ���ż��ģ8����ô���佫�ԡ�long����λ���С�
// �����Ч��ַ�ͳ�����(resp.)ż��ģ4��2�������ԡ�int����short��Ϊ��λ��
    public native void copyMemory(Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes);

    // �������ڴ���е������ֽ�����Ϊ��һ����ĸ��������ṩ��һ��<em>���Ĵ���</em>Ѱַģʽ��
    // ��{@link #getInt(Object,long)}�������۵ġ�
// �൱��<code>copyMemory(null, srcAddress, null, destAddress, bytes)</code>��
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        copyMemory(null, srcAddress, null, destAddress, bytes);
    }

    // ��{@link #allocateMemory}��{@link #reallocateMemory}��õı����ڴ������á�
    // ���ݸ��˷����ĵ�ַ����Ϊnull������������²���ȡ�κβ�����
    public native void freeMemory(long address);

// �������


    public static final int INVALID_FIELD_OFFSET = -1;

    /**
     * @deprecated ��1.4.1��ʼ����̬�ֶ�ʹ��{@link #staticFieldOffset}��
     * �Ǿ�̬�ֶ�ʹ��{@link #objectFieldOffset}��
     */
    @Deprecated
    public int fieldOffset(Field f) {
        if (Modifier.isStatic(f.getModifiers()))
            return (int) staticFieldOffset(f);
        else
            return (int) objectFieldOffset(f);
    }

    /**
     * @deprecated �������ڷ��ʸ������е�ĳЩ��̬�ֶεĻ�����ַ��
     * <p>
     * ��1.4.1��ʼ��ʹ��{@link #staticFieldBase(Field)}��������ض�{@link�ֶ�}�Ļ����˷����������ڽ�����������о�̬��Ϣ�洢��һ��λ�õ�jvm��
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

    // ��������ֶ�������Ĵ洢�����е�λ�á���Ҫ���������ƫ������ִ���κ�����;
    // ��ֻ��һ�������ݸ�����ȫ�Ķ��ڴ��������cookie��
// �κθ������ֶζ���ʼ�վ�����ͬ��ƫ�����ͻ���������ͬ���������ͬ�ֶζ����������ͬ��ƫ�����ͻ���
// ��1.4.1��ʼ���ֶε�ƫ��������ʾΪlongֵ������Sun JVMû��ʹ������Ҫ��32λ��
// ���ǣ�����̬�ֶδ洢�ھ��Ե�ַ��JVMʵ�ֿ���ʹ�ó�ƫ�����Ϳջ�ָ������{@link #getInt(Object,long)}���õ���ʽ��ʾ�ֶ�λ�á�
// ��ˣ�Ҫ��64λƽ̨����ֲ������jvm�Ĵ�����뱣�����о�̬�ֶ�ƫ������
    public native long staticFieldOffset(Field f);

    // ��{@link #staticFieldBase}һ�𱨸������̬�ֶε�λ�á�
// ��Ҫ���������ƫ������ִ���κ�����;��ֻ��һ�������ݸ�����ȫ�Ķ��ڴ��������cookie��
// �κθ������ֶζ�������ͬ��ƫ����������ͬһ�����������ͬ���ֶζ���������ͬ��ƫ������
    public native long objectFieldOffset(Field f);

    // ��1.4.1��ʼ���ֶε�ƫ��������ʾΪlongֵ������Sun JVMû��ʹ������Ҫ��32λ��
    // ��������JVM������Ҫ�ڷ���������б���һ��ƫ���������ǣ�Ϊ����������е�������������һ�£�
    // ����������������Ϊһ��longֵ��
// ��ȡ��������(����еĻ�)��ʹ��������ͨ��{@link #getInt(Object, long)}֮��ķ������ʸ�����ľ�̬�ֶΡ�
// ��ֵ����Ϊ�ա����ֵ��������һ������
// ����һ����cookie�������ܱ�֤����һ�������Ķ�������Ӧ�����κη�ʽʹ�ã�������Ϊ������е�get��put���̵Ĳ�����
    public native Object staticFieldBase(Field f);

    // �����������Ƿ���Ҫ��ʼ������ͨ�����ȡ��ľ�̬�ֶλ�һ��ʹ�á�
    public native boolean shouldBeInitialized(Class<?> c);

    // ȷ���ѳ�ʼ���������ࡣ��ͨ�����ȡ��ľ�̬�ֶλ�һ��ʹ�á�
    public native void ensureClassInitialized(Class<?> c);

    // �������������Ĵ洢�����е�һ��Ԫ�ص�ƫ���������{@link #arrayIndexScale}Ϊͬһ���෵��һ������ֵ��
    // �����ʹ�øñ������Ӻ������ƫ�������γ��µ�ƫ�������Է��ʸ����������Ԫ�ء�
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

    // ����ͨ��{@link #putAddress}�洢�ı���ָ��Ĵ�С(���ֽ�Ϊ��λ)�����ֵ����4��8��
    // ע�⣬�����������͵Ĵ�С(�洢�ڱ����ڴ����)��ȫ�����ǵ���Ϣ���ݾ�����
    public native int addressSize();

    /**
     * The value of {@code addressSize()}
     */
    public static final int ADDRESS_SIZE = theUnsafe.addressSize();

    // ���ֽ�Ϊ��λ���汾���ڴ�ҳ�Ĵ�С(��������ʲô)�����ֵ����2���ݡ�
    public native int pageSize();


    /// random trusted operations from JNI:

    // ����VM����һ���࣬����Ҫ���а�ȫ��顣Ĭ������£���װ�����ͱ��������Ե����ߵ��ࡣ
    public native Class<?> defineClass(String name, byte[] b, int off, int len,
                                       ClassLoader loader,
                                       ProtectionDomain protectionDomain);

// ����һ���࣬����Ҫ����װ������ϵͳ�ֵ�֪������
// ����ÿ��CP��Ŀ����Ӧ��CP����������null�������������ı�ǩƥ���a��ʽ:
    public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);


    // ����һ��ʵ�������������κι��캯���������û���࣬���ʼ������
    public native Object allocateInstance(Class<?> cls)
            throws InstantiationException;

    /** @deprecated
    ��������������ͨ��{@link #monitorExit}������*/
    public native void monitorEnter(Object o);

    /** @deprecated
    �ͷŶ���������ͨ��{@link #monitorEnter}��������*/
    public native void monitorExit(Object o);

    /** @deprecated
    ��ͼ�������󡣷���true��false��ָʾ���Ƿ�ɹ�������ǣ������ͨ��{@link #monitorExit}�����ö���*/
    public native boolean tryMonitorEnter(Object o);

    // �ڲ�֪ͨ��֤�ߵ�������׳��쳣
    public native void throwException(Throwable ee);

// �Ƚ����滻����
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

    // ȡ��������<tt>park</tt>�������ĸ����̣߳����ߣ����û����������������<tt>park</tt>�������ĵ��á�ע��:��������ǡ�����ȫ�ġ���������Ϊ�����߱�����ĳ�ַ�ʽȷ���߳�û�б��ƻ���
    // ����Java����ʱ(����ͨ���ж��̵߳Ļ�����)��ͨ������Ҫ�κ�����Ķ�����ȷ����һ�㣬���ǵ��ӱ����������ʱ���ⲻ�Ǽ����Զ��ġ�
    public native void unpark(Object thread);

    // ������ǰ�߳�,����ʱƽ��< tt > unpark < / tt >����,��ƽ��< tt > unpark < / tt >�Ѿ�����,���̱߳��ж�,����,������Ǿ��Ժ�ʱ�䲻Ϊ��,
    // ����ʱ����������,�����������,�����������Ժ���Ϊ��λ��ʱ���Ѿ���ȥ,��ò��(������û�С����ɡ��ػ���)��ע��:����������ڲ���ȫ���У���Ϊ<tt>unpark</tt>�ǣ����԰������������ط������֡�
    public native void park(boolean isAbsolute, long time);

    // ��ȡ��������ô�������ϵͳ���ж����и�ʱ��ε�ƽ�����ء��÷�������������<tt>nelem</tt>�������������������<tt>loadavg</tt>�����Ԫ�ء�ϵͳ���ʩ��3���������ֱ�����ȥ1���ӡ�5���Ӻ�15���ӵ�ƽ��ֵ��
// @param loadavg loadavg��һ��˫����С�Ĵ�������
// @param nelems ���ص���������������1��3��
// @return ʵ�ʼ���������������;����޷���ø���ƽ��ֵ����Ϊ-1��
    public native int getLoadAverage(double[] loadavg, int nelems);

// �����������ʵ����Javaʵ��
// ��֧�ֱ���ָ���ƽ̨

    // �ڸ����Ĵ���ƫ����ʱ��ԭ�ӵؽ�������ֵ��ӵ���������<code>o</code> >�е��ֶλ�����Ԫ�صĵ�ǰֵ��
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

    // �ڸ����Ĵ���>ƫ����</code>ʱ��ԭ�ӵؽ�������ֵ��ӵ���������<code>o</code> >�е��ֶλ�����Ԫ�صĵ�ǰֵ��
    public final long getAndAddLong(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }

    // �ڸ����Ĵ���>ƫ����</code>����ԭ�ӵؽ�������ֵ���������<code>o</code> >�е��ֶλ�����Ԫ�صĵ�ǰֵ���н�����
    public final int getAndSetInt(Object o, long offset, int newValue) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, newValue));
        return v;
    }

    // �ڸ����Ĵ���>ƫ����</code>����ԭ�ӵؽ�������ֵ���������<code>o</code> >�е��ֶλ�����Ԫ�صĵ�ǰֵ���н�����
    public final long getAndSetLong(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, newValue));
        return v;
    }

    // �ڸ����Ĵ���>ƫ����</code>����
    // ԭ�ӵؽ������Ĳο�ֵ���������<code>o</code> >�е��ֶλ�����Ԫ�صĵ�ǰ�ο�ֵ���н�����
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
