package com.baidu.shop.entity;

import com.baidu.shop.validate.group.MingruiOperation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @ClassName BrandEntity
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/8/31
 * @Version V1.0
 **/
@Data
@Table(name = "tb_brand")
@ApiModel(value = "品牌实体类")
public class BrandEntity {

    @Id
    @ApiModelProperty(value = "品牌id" , example = "1")
    @NotNull(message = "主键不能为空" , groups = {MingruiOperation.Update.class})
    @GeneratedValue(strategy = GenerationType.IDENTITY) //声明的主键生成策略
    private Integer id;

    @ApiModelProperty(value = "品牌名称")
    @NotEmpty(message = "名牌名称不能为空", groups = {MingruiOperation.Add.class,MingruiOperation.Update.class})
    private String name;

    @ApiModelProperty(value = "品牌logo")
    private String image;

    @ApiModelProperty(value = "品牌首字母")
    private Character letter;

}
