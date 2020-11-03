package com.example.demo.service.impl;

import com.example.demo.dao.IStuffDao;
import com.example.demo.service.IStuffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class StuffService implements IStuffService {

    @Autowired
    private IStuffDao stuffDao;

//    public StuffService() {
//        System.out.println(stuffDao.getClass());
//    }

    @PostConstruct
    void postInit() {
        System.out.println(stuffDao.getClass());
    }

}
