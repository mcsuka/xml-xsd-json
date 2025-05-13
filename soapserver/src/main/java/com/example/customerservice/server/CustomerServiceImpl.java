/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.example.customerservice.server;

import java.util.*;

import com.example.customerservice.*;
import jakarta.annotation.Resource;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.WebServiceContext;

public class CustomerServiceImpl implements CustomerService {

    /**
     * The WebServiceContext can be used to retrieve special attributes like the
     * user principal. Normally it is not needed
     */
    @Resource
    WebServiceContext wsContext;

    Map<Integer, Customer> customers = new HashMap<>();

    public List<Customer> getCustomersByName(String name) {
        return customers.values().stream().filter(c -> c.getName().equals(name)).toList();
    }

    public Customer getCustomer(int customerId) {
        return customers.get(customerId);
    }

    public Customer deleteCustomer(int customerId) {
        return customers.remove(customerId);
    }


    public String updateCustomer(Customer customer) {
        System.out.println("update request was received");
        Customer oldCust =  customers.put(customer.getCustomerId(), customer);
        System.out.println("Customer was updated");
        return (oldCust == null ? "CREATED" : "UPDATED");
    }

}
