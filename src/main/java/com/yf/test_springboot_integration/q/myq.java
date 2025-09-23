package com.yf.test_springboot_integration.q;

import com.yf.partition.Partition;
import com.yf.springboot_integration.pool.annotation.PartiResource;

/**
 * @author yyf
 * @description
 */
@PartiResource("myq")
public class myq extends Partition {

    @Override
    public Boolean offer(Object t) {
        return null;
    }

    @Override
    public Runnable poll(Integer waitTime) throws InterruptedException {
        return null;
    }

    @Override
    public Boolean removeEle() {
        return null;
    }


    @Override
    public int getEleNums() {
        return 0;
    }

    @Override
    public void lockGlobally() {

    }

    @Override
    public void unlockGlobally() {

    }

    @Override
    public Integer getCapacity() {
        return 0;
    }

    @Override
    public void setCapacity(Integer capacity) {

    }

}
