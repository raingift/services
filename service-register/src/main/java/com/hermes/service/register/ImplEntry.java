package com.hermes.service.register;

public class ImplEntry<T> {

    public String implName;
    public T implClazz;

    public ImplEntry(String implName, T implClazz) {
        this.implName = implName;
        this.implClazz = implClazz;
    }
}
