package com.mr.entity;

/**
 * @ClassName Student
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/9/14
 * @Version V1.0
 **/
public class Student {

    private String code;

    private String pass;

    private Integer age;

    private String likeColor;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getLikeColor() {
        return likeColor;
    }

    public void setLikeColor(String likeColor) {
        this.likeColor = likeColor;
    }

    @Override
    public String toString() {
        return "Student{" +
                "code='" + code + '\'' +
                ", pass='" + pass + '\'' +
                ", age=" + age +
                ", likeColor='" + likeColor + '\'' +
                '}';
    }

    public Student(String code, String pass, Integer age, String likeColor) {
        this.code = code;
        this.pass = pass;
        this.age = age;
        this.likeColor = likeColor;
    }

    public Student() {
    }
}
