package com.mr.test;

import com.mr.RunTestEsApplication;
import com.mr.entity.GoodsEntity;
import com.mr.repository.GoodsEsRepository;
import com.mr.util.ESHighLightUtil;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName TestEs
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/9/14
 * @Version V1.0
 **/

//让测试在Spring容器环境下执行
@RunWith(SpringRunner.class)
//声明启动类,当测试方法运行的时候会帮我们自动启动容器
@SpringBootTest(classes = { RunTestEsApplication.class})
public class TestEs {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private GoodsEsRepository goodsEsRepository;

    /*
    创建索引
     */
    @Test
    public void createGoodsIndex(){
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(IndexCoordinates.of("indexname"));

        if (!indexOperations.exists()){
            indexOperations.create();//创建索引
        }

        //indexOperations.exists() 判断索引是否存在
        System.out.println(indexOperations.exists()?"索引创建成功":"索引创建失败");
    }

        /*
    创建映射
     */
    @Test
    public void createGoodsMapping(){

        //此构造函数会检查有没有索引存在,如果没有则创建该索引,如果有则使用原来的索引
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsEntity.class);
        //indexOperations.create();
        //indexOperations.createMapping();//创建映射,不调用此函数也可以创建映射,这就是高版本的强大之处
        System.out.println("映射创建成功" + indexOperations.exists());
    }

    /*
    删除索引
     */
    @Test
    public void deleteGoodsIndex(){
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsEntity.class);

        indexOperations.delete();
        System.out.println("索引删除成功");
    }

    /*
    新增文档
     */
    @Test
    public void saveData(){

        GoodsEntity entity = new GoodsEntity();
        entity.setId(1L);
        entity.setBrand("小米");
        entity.setCategory("手机");
        entity.setImages("xiaomi.jpg");
        entity.setPrice(1000D);
        entity.setTitle("小米4");

        //save可以做新增,也可以做修改
        //通过id没有查询到数据->新增操作
        //通过id查询到数据->修改操作
        //goodsEsRepository.save(entity);

        //也可以做新增 删除 保存操作
        elasticsearchRestTemplate.save(entity);

        //elasticsearchRestTemplate.delete(entity);

        System.out.println("新增成功");
    }

    /*
   查询所有
    */
    @Test
    public void searchAll(){
        //查询总条数
        long count = goodsEsRepository.count();
        System.out.println(count);
        //查询所有数据
        Iterable<GoodsEntity> all = goodsEsRepository.findAll();
        all.forEach(goods -> {
            System.out.println(goods);
        });

        List<GoodsEntity> byPriceBetween = goodsEsRepository.findByPriceBetween(3000D, 4000D);
        System.out.println(byPriceBetween);

//        List<GoodsEntity> test = goodsEsRepository.findByTitle("小米4");
//
//        List<GoodsEntity> byTitleAAndBrand = goodsEsRepository.findByTitleAndBrand("小米4", "小米");
//        System.out.println(test);
    }

    /*
    批量新增文档
     */
    @Test
    public void saveAllData(){

        GoodsEntity entity = new GoodsEntity();
        entity.setId(2L);
        entity.setBrand("苹果");
        entity.setCategory("手机");
        entity.setImages("pingguo.jpg");
        entity.setPrice(5000D);
        entity.setTitle("iphone11手机");

        GoodsEntity entity2 = new GoodsEntity();
        entity2.setId(3L);
        entity2.setBrand("三星");
        entity2.setCategory("手机");
        entity2.setImages("sanxing.jpg");
        entity2.setPrice(3000D);
        entity2.setTitle("w2019手机");

        GoodsEntity entity3 = new GoodsEntity();
        entity3.setId(4L);
        entity3.setBrand("华为");
        entity3.setCategory("手机");
        entity3.setImages("huawei.jpg");
        entity3.setPrice(4000D);
        entity3.setTitle("华为mate30手机");

        ArrayList<GoodsEntity> goodsEntities = new ArrayList<>();

        goodsEntities.add(entity);
        goodsEntities.add(entity2);
        goodsEntities.add(entity3);

        goodsEsRepository.saveAll(goodsEntities);

        //goodsEsRepository.saveAll(Arrays.asList(entity,entity2,entity3));

        System.out.println("批量新增成功");
    }

    @Test
    public void seach(){

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        //nativeSearchQueryBuilder.withQuery(QueryBuilders.matchQuery("title","小米"));

        nativeSearchQueryBuilder.withQuery(
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("title","小米手机"))
        );

        SearchHits<GoodsEntity> search = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), GoodsEntity.class);

        List<SearchHit<GoodsEntity>> searchHits = search.getSearchHits();
        searchHits.stream().forEach(hit -> {
            GoodsEntity entity = hit.getContent();
            System.out.println(hit.getContent());
        });

    }

    //高亮
    @Test
    public void customizeSearchHighLight(){

        //构建高级查询
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        //构建高亮查询
        HighlightBuilder highlightBuilder = new HighlightBuilder();

        //设置高亮查询字段
        HighlightBuilder.Field title = new HighlightBuilder.Field("title");

        //设置高亮标签
        title.preTags("<span style=color:red'>");
        title.postTags("</span>");
        highlightBuilder.field(title);

        //高亮查询
        nativeSearchQueryBuilder.withHighlightBuilder(highlightBuilder);

        nativeSearchQueryBuilder.withHighlightBuilder(highlightBuilder);

        nativeSearchQueryBuilder.withQuery(
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("title","小米"))
                        //.must(QueryBuilders.rangeQuery("price").gte(1000).lte(10000))
        );

        SearchHits<GoodsEntity> search = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), GoodsEntity.class);

        List<SearchHit<GoodsEntity>> searchHits = search.getSearchHits();
        List<GoodsEntity> title2 = searchHits.stream().map(hit -> {
            GoodsEntity entity = hit.getContent();

            //通过字段名获取高亮查询结果
            List<String> title1 = hit.getHighlightField("title");

            entity.setTitle(title1.get(0));
            return entity;
        }).collect(Collectors.toList());

        System.out.println(title2);
    }

    /*
    使用工具类
     */
    @Test
    public void customizeSearchHighLightUtil(){

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        //构建高亮查询
        queryBuilder.withHighlightBuilder(ESHighLightUtil.getHighlightBuilder("title"));//设置高亮

        queryBuilder.withQuery(
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("title","华为手机"))
                        .must(QueryBuilders.rangeQuery("price").gte(1000).lte(10000))
        );

        //排序
        queryBuilder.withSort(SortBuilders.fieldSort("price").order(SortOrder.DESC));

        //分页 当前页-1
        queryBuilder.withPageable(PageRequest.of(0,10));

        SearchHits<GoodsEntity> search = elasticsearchRestTemplate.search(queryBuilder.build(), GoodsEntity.class);

        List<SearchHit<GoodsEntity>> searchHits = search.getSearchHits();

        //重新设置title
        List<SearchHit<GoodsEntity>> highLightHit = ESHighLightUtil.getHighLightHit(searchHits);

        System.out.println(highLightHit);
    }

}
