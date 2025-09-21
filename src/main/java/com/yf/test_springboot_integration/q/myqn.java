package com.yf.test_springboot_integration.q;

import com.yf.springboot_integration.pool.annotation.PartiResource;
import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @description
 */
@PartiResource("myqn")
public class myqn extends Partition {
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
