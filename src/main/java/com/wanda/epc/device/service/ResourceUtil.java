package com.wanda.epc.device.service;


import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href:"mailto:oujunxiao.ou@zkteco.com">oujunxiao</a>
 * @version v1.0
 */
public class ResourceUtil {
    private static ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     *
     * 从jar、项目目录中读取资源文件
     * <p>
     * 使用说明：<br>
     * 1：*.txt:将返回类路径下的所有.txt文件<br>
     * 2：conf/*.txt：将返回类路径与Jar文件conf下的所有.txt文件<br>
     * 3：asm.txt:将返回类路径与Jar文件下的asm.txt文件<br>
     * <p>
     *
     * @param locationPattern 模式匹配
     * @author flywind.wang
     * @return 返回匹配到的文件的Resource数组
     */
    public static Resource[] loadResources(String locationPattern)
    {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try
        {
            resources = resolver.getResources("classpath*:" + locationPattern);
            return resources;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * 从jar、项目目录中读取资源文件,至返回一个
     * <p>
     * 使用说明：<br>
     * 1：*.txt:将返回类路径下的所有.txt文件<br>
     * 2：conf/*.txt：将返回类路径与Jar文件conf下的所有.txt文件<br>
     * 3：asm.txt:将返回类路径与Jar文件下的asm.txt文件<br>
     * <p>
     *
     * @param locationPattern 模式匹配
     * @author flywind.wang
     * @return 返回匹配到的文件的Resource
     */
    public static Resource loadResourcesOne(String locationPattern)
    {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try
        {
            resources = resolver.getResources("classpath*:" + locationPattern);
            if (resources != null && resources.length > 0)
            {
                return resources[0];
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     *
     * 从jar、项目目录中读取资源文件
     * <p>
     * 使用说明：<br>
     * 1：*.txt:将返回类路径下的所有.txt文件<br>
     * 2：conf/*.txt：将返回类路径与Jar文件conf下的所有.txt文件<br>
     * 3：asm.txt:将返回类路径与Jar文件下的asm.txt文件<br>
     * <p>
     *
     * @param locationPattern 模式匹配
     * @author flywind.wang
     * @return 返回匹配到的文件的InputStream集合
     */
    public static List<InputStream> loadResourcesAsInputStream(String locationPattern)
    {
        List<InputStream> xmlInputStreamList = new ArrayList<InputStream>();
        Resource[] resources = loadResources(locationPattern);
        for (Resource resource : resources)
        {
            try
            {
                xmlInputStreamList.add(resource.getInputStream());
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        return xmlInputStreamList;
    }

    /**
     *
     * 从jar、项目目录中读取资源文件，只返回一个
     * <p>
     * 使用说明：<br>
     * 1：*.txt:将返回类路径下的所有.txt文件<br>
     * 2：conf/*.txt：将返回类路径与Jar文件conf下的所有.txt文件<br>
     * 3：asm.txt:将返回类路径与Jar文件下的asm.txt文件<br>
     * <p>
     *
     * @param locationPattern 模式匹配
     * @author flywind.wang
     * @return 返回匹配到的文件的InputStream
     */
    public static InputStream loadResourcesOneAsInputStream(String locationPattern)
    {
        Resource resource = loadResourcesOne(locationPattern);
        if (resource != null)
        {
            try
            {
                return resource.getInputStream();
            }
            catch (IOException e)
            {
                ;
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     *
     * 读取资源
     * <p>
     * 使用说明：<br>
     * 1：file:C:/test.dat<br>
     * 2：classpath:test.dat<br>
     * 3：WEB-INF/test.dat
     * <p>
     *
     * @param location 路径
     * @author flywind.wang
     * @return 返回匹配到的文件的resource
     */
    public static Resource getResource(String location)
    {
        Resource resource = resolver.getResource(location);
        return resource;
    }

    /**
     * 从classpath*路径下获取资源,支持通配符
     *
     * @param locationPattern 模式匹配
     * @return 返回匹配到的文件的Resource数组
     */
    public static Resource[] getResources(String locationPattern) throws IOException {
        if (!locationPattern.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
            locationPattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + locationPattern;
        }
        return resolver.getResources(locationPattern);
    }
}
