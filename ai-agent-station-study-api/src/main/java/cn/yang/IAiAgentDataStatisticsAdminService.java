package cn.yang;

import cn.yang.dto.DataStatisticsResponseDTO;
import cn.yang.response.Response;

/**
 * 数据统计
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/10/4 10:33
 */
public interface IAiAgentDataStatisticsAdminService {

    /**
     * 获取系统数据统计
     * @return 统计数据响应
     */
    Response<DataStatisticsResponseDTO> getDataStatistics();
}
