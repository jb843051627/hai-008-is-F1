package com.fc.v2.service.haz;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 最小测试装配：H2 内存库（MODE=MySQL）+ MyBatis-Plus + 声明式事务。
 * 不引 shiro/redis/druid/quartz，只测派发领域。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Configuration
@EnableTransactionManagement
@MapperScan("com.fc.v2.mapper.auto")
public class HazTestConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:haz;MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    @PostConstruct
    public void initSchema() throws Exception {
        try (Connection c = dataSource().getConnection()) {
            ScriptUtils.executeSqlScript(c,
                    new org.springframework.core.io.ClassPathResource("schema-h2.sql"));
        }
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean f = new MybatisSqlSessionFactoryBean();
        f.setDataSource(dataSource);
        f.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mybatis/**/*Mapper.xml"));
        f.setTypeAliasesPackage("com.fc.v2.model.auto");
        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(cfg);
        f.setGlobalConfig(new GlobalConfig());
        return f.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
