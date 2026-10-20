package com.logistics.oms.module.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.common.PageResult;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.mapper.CustomerMapper;
import com.logistics.oms.module.order.entity.OrderMain;
import com.logistics.oms.module.order.mapper.OrderMainMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final OrderMainMapper orderMainMapper;

    public CustomerService(CustomerMapper customerMapper, OrderMainMapper orderMainMapper) {
        this.customerMapper = customerMapper;
        this.orderMainMapper = orderMainMapper;
    }

    /** 全部启用客户（下拉框用）。 */
    public List<Customer> listActive() {
        return customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getStatus, 1)
                .orderByAsc(Customer::getId));
    }

    public PageResult<Customer> page(long current, long size, String keyword) {
        LambdaQueryWrapper<Customer> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(x -> x.like(Customer::getCompanyName, keyword)
                    .or().like(Customer::getCustomerCode, keyword));
        }
        w.orderByDesc(Customer::getId);
        return PageResult.of(customerMapper.selectPage(new Page<>(current, size), w));
    }

    public Customer create(Customer customer) {
        if (customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCustomerCode, customer.getCustomerCode())) > 0) {
            throw BusinessException.param("客户编码已存在");
        }
        customer.setStatus(customer.getStatus() == null ? 1 : customer.getStatus());
        customerMapper.insert(customer);
        return customer;
    }

    public Customer update(Long id, Customer req) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw BusinessException.notFound("客户不存在");
        }
        customer.setCompanyName(req.getCompanyName());
        customer.setContact(req.getContact());
        customer.setCountry(req.getCountry());
        customer.setStatus(req.getStatus());
        customerMapper.updateById(customer);
        return customer;
    }

    public void delete(Long id) {
        if (customerMapper.selectById(id) == null) {
            throw BusinessException.notFound("客户不存在");
        }
        if (orderMainMapper.selectCount(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getCustomerId, id)) > 0) {
            throw BusinessException.conflict("该客户存在关联订单，不能删除");
        }
        customerMapper.deleteById(id);
    }
}
