package org.EnoTurEPO.util;

/**
 * Data estructure for a group of three objects
 * @author Rodrigo Currás
 */
public class Trio<T1,T2,T3> {
    T1 obj1;
    T2 obj2;
    T3 obj3;

    /**Constructor
     * @param obj1 First object
     * @param obj2 Second object
     * @param obj3 Third object
     */
    public Trio(T1 obj1, T2 obj2, T3 obj3){
        this.obj1 = obj1;
        this.obj2 = obj2;
        this.obj3 = obj3;
    }

    /**
     * @return Returns the obj1
     */
    public T1 getObj1(){
        return obj1;
    }

    /**
     * @param obj1 The obj1 to set
     */
    public void setObj1(T1 obj1){
        this.obj1 = obj1;
    }

    /**
     * @return Returns the obj2
     */
    public T2 getObj2(){
        return obj2;
    }

    /**
     * @param obj2 The obj2 to set
     */
    public void setObj2(T2 obj2){
        this.obj2 = obj2;
    }

    /**
     * @return Returns the obj3
     */
    public T3 getObj3(){
        return obj3;
    }

    /**
     * @param obj3 The obj3 to set
     */
    public void setObj3(T3 obj3){
        this.obj3 = obj3;
    }
}