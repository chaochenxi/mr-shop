package com.mr.controller;

import com.mr.entity.Student;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

/**
 * @ClassName TestController
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/9/14
 * @Version V1.0
 **/
@Controller//controller可以返回页面
public class TestController {

    @GetMapping("test")//定义url使用modelmap返回数据
    public String test(ModelMap map){
        map.put("name","tomcat");
        return "test";
    }
    /**
     * 展示学生
     * @param map
     * @return
     */
    @GetMapping("stu")
    public String student(ModelMap map){
        Student student=new Student();
        student.setCode("007");
        student.setPass("9527");
        student.setAge(12);
        student.setLikeColor("<font color='red'>红色</font>");
        map.put("stu",student);
        return "student";
    }

    @GetMapping("list")
    public String list(ModelMap map){
        Student s1=new Student("001","111",18,"red");
        Student s2=new Student("002","222",19,"red");
        Student s3=new Student("003","333",16,"blue");
        Student s4=new Student("004","444",28,"blue");
        Student s5=new Student("005","555",68,"blue");

        //转为List
        map.put("stuList", Arrays.asList(s1,s2,s3,s4,s5));
        return "list";
    }

}

