package com.anon.anonrpc.serializer;

import java.io.*;

/**
 * JDK 序列化器
 */
public class JdkSerializer implements Serializer {

    /**
     * 序列化
     *
     * @param object
     * @param <T>
     * @return
     * @throws IOException
     */
    @Override
    public <T> byte[] serialize(T object) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.close();
        return outputStream.toByteArray();
    }

    /**
     * 反序列化
     *
     * @param bytes
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
//    @Override
//    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
//        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
//        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
//        try {
//            return (T) objectInputStream.readObject();
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        } finally {
//            objectInputStream.close();
//        }
//
//    }
    @Override
    public <T > T deserialize( byte[] bytes, Class<T > type) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException("反序列化数据为空");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (T) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("类未找到", e);
        } catch (EOFException e) {
            throw new RuntimeException("反序列化数据不完整或损坏", e);
        }
    }
}
