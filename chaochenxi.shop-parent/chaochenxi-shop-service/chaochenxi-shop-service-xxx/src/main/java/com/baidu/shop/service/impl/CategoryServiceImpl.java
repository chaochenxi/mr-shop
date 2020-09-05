package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.entity.CategoryBrandEntity;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.entity.SpecGroupEntity;
import com.baidu.shop.mapper.CategoryBrandMapper;
import com.baidu.shop.mapper.CategoryMapper;
import com.baidu.shop.mapper.SpecGroupMapper;
import com.baidu.shop.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.List;

/**
 * @ClassName CategoryServiceImpl
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/8/28
 * @Version V1.0
 **/
@RestController
public class CategoryServiceImpl extends BaseApiService implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SpecGroupMapper specGroupMapper;

    @Resource
    private CategoryBrandMapper categoryBrandMapper;

    @Override
    public Result<List<CategoryEntity>> getCategoryByPid(Integer pid) {

        CategoryEntity categoryEntity = new CategoryEntity();

        categoryEntity.setParentId(pid);

        List<CategoryEntity> list = categoryMapper.select(categoryEntity);

        return this.setResultSuccess(list);
    }

    @Transactional
    @Override
    public Result<JSONObject> saveCategory(CategoryEntity categoryEntity) {

        //通过新增节点的父id将父节点的parent状态改成1
        CategoryEntity categoryEntity1 = new CategoryEntity();
        categoryEntity1.setId(categoryEntity.getParentId());
        categoryEntity1.setIsParent(1);
        categoryMapper.updateByPrimaryKeySelective(categoryEntity1);

        categoryMapper.insertSelective(categoryEntity);

        return this.setResultSuccess();
    }

    @Transactional
    @Override
    public Result<JSONObject> editCategory(CategoryEntity categoryEntity) {

        categoryMapper.updateByPrimaryKeySelective(categoryEntity);

        return this.setResultSuccess();
    }

    @Transactional
    @Override
    public Result<JSONObject> deleteCategory(Integer id) {

        //通过当前id查询分类信息
        CategoryEntity categoryEntity = categoryMapper.selectByPrimaryKey(id);
        if(categoryEntity == null){
            return this.setResultError("当前id不存在");
        }
        //判断当前节点是不是父节点
        if(categoryEntity.getIsParent() == 1){
            return this.setResultError("当前节点为父id,不能删除");
        }

        //构建条件查询,通过当前被删除节点的parentid查询数据
        Example example = new Example(CategoryEntity.class);
        example.createCriteria().andEqualTo("parentId",categoryEntity.getParentId());
        List<CategoryEntity> list = categoryMapper.selectByExample(example);
        //判断查询出的数据是否只有一条
        if(list.size() == 1){
            CategoryEntity entity = new CategoryEntity();
            entity.setId(categoryEntity.getParentId());
            //将父节点的isParent状态改为0
            entity.setIsParent(0);
            categoryMapper.updateByPrimaryKeySelective(entity);
        }

        Example example1 = new Example(SpecGroupEntity.class);
        example1.createCriteria().andEqualTo("cid",id);
        List<SpecGroupEntity> list1 = specGroupMapper.selectByExample(example1);
        if(list1.size() > 0){
            return this.setResultError("此分类绑定规格不能删除");
        }

        Example example2 = new Example(CategoryBrandEntity.class);
        example2.createCriteria().andEqualTo("categoryId",id);
        List<CategoryBrandEntity> list2 = categoryBrandMapper.selectByExample(example2);
        if(list2.size() > 0){
            return this.setResultError("此分类绑定品牌不能删除");
        }

        categoryMapper.deleteByPrimaryKey(id);

        return this.setResultSuccess();
    }

    @Override
    public Result<List<CategoryEntity>> getByBrand(Integer brandId) {
        List<CategoryEntity> byBrandId = categoryMapper.getByBrandId(brandId);

        return this.setResultSuccess(byBrandId);
    }

}
