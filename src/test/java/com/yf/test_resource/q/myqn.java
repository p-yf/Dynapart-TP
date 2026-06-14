package com.yf.test_resource.q;

import com.yf.core.resource_container.scanned_annotation.PartiResource;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @description 测试用自定义队列2
 */
@PartiResource("myqn")
public class myqn extends Partition {
    @Override
    public boolean offer(Object t) {
        return false;
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

    @Override
    public void markAsSwitched() {
    }

    @Override
    public int drainTo(Partition target) {
        return 0;
    }

}
