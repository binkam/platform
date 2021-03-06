/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.sample;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;

public class CustomerUtil {

    public static Customer fromOM(OMElement element) {
        Customer customer = new Customer();

        OMElement child;

        child = element.getFirstChildWithName(new QName(Customer.NS_URI,
                "first"));
        customer.setFirst(child.getText());

        child = element
                .getFirstChildWithName(new QName(Customer.NS_URI, "last"));
        customer.setLast(child.getText());

        child = element.getFirstChildWithName(new QName(Customer.NS_URI,
                "address"));
        customer.setAddress(child.getText());

        child = element.getFirstChildWithName(new QName(Customer.NS_URI,
                "city"));
        customer.setCity(child.getText());

        child = element.getFirstChildWithName(new QName(Customer.NS_URI,
                "state"));
        customer.setState(child.getText());

        child = element
                .getFirstChildWithName(new QName(Customer.NS_URI, "zip"));
        customer.setZip(child.getText());

        return customer;
    }

    public static OMElement toOM(Customer customer) {

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement customerElement = factory.createOMElement("Customer",
                Customer.NS_URI, "xxx");

        OMElement e;

        e = factory.createOMElement("first", Customer.NS_URI, "xxx");
        e.setText(customer.getFirst());
        customerElement.addChild(e);

        e = factory.createOMElement("last", Customer.NS_URI, "xxx");
        e.setText(customer.getLast());
        customerElement.addChild(e);

        e = factory.createOMElement("address", Customer.NS_URI, "xxx");
        e.setText(customer.getAddress());
        customerElement.addChild(e);

        e = factory.createOMElement("city", Customer.NS_URI, "xxx");
        e.setText(customer.getCity());
        customerElement.addChild(e);

        e = factory.createOMElement("state", Customer.NS_URI, "xxx");
        e.setText(customer.getState());
        customerElement.addChild(e);

        e = factory.createOMElement("zip", Customer.NS_URI, "xxx");
        e.setText(customer.getZip());
        customerElement.addChild(e);

        return customerElement;
    }

}
