/*
 * ****************************************************************************
 * Copyright VMware, Inc. 2010-2016.  All Rights Reserved.
 * ****************************************************************************
 *
 * This software is made available for use under the terms of the BSD
 * 3-Clause license:
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.vmware.events;

import com.vmware.common.annotations.Action;
import com.vmware.common.annotations.Sample;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.vim25.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * EventHistoryCollectorMonitor
 *
 * This sample demonstrates how to create and monitor an EventHistoryCollector
 * This sample uses the latestPage property of the EventHistoryCollector
 * to filter the Events
 *
 * <b>Parameters:</b>
 * url            [required] : url of the web service
 * username       [required] : username for the authentication
 * password       [required] : password for the authentication
 *
 * <b>Command Line:</b>
 * run.bat com.vmware.vm.EventHistoryCollectorMonitor
 * --url [webserviceurl] --username [username] --password [password]
 * </pre>
 */

@Sample(
        name = "event-history-collector-monitor",
        description = "This sample demonstrates how to create and monitor an EventHistoryCollector " +
                "This sample uses the latestPage property of the EventHistoryCollector " +
                "to filter the Events"
)
public class EventHistoryCollectorMonitor extends ConnectedVimServiceBase {
    private ManagedObjectReference propCollector;
    private ManagedObjectReference eventManager;
    private ManagedObjectReference eventHistoryCollector;

    void initEventManagerRef() {
        if (serviceContent != null) {
            eventManager = serviceContent.getEventManager();
        }
    }

    void createEventHistoryCollector() throws RuntimeFaultFaultMsg, InvalidStateFaultMsg {
        EventFilterSpec eventFilter = new EventFilterSpec();
        eventHistoryCollector =
                vimPort.createCollectorForEvents(eventManager, eventFilter);
    }

    PropertyFilterSpec createEventFilterSpec() {
        PropertySpec propSpec = new PropertySpec();
        propSpec.setAll(new Boolean(false));
        propSpec.getPathSet().add("latestPage");
        propSpec.setType(eventHistoryCollector.getType());

        ObjectSpec objSpec = new ObjectSpec();
        objSpec.setObj(eventHistoryCollector);
        objSpec.setSkip(new Boolean(false));

        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.getPropSet().add(propSpec);
        spec.getObjectSet().add(objSpec);
        return spec;
    }

    /**
     * Uses the new RetrievePropertiesEx method to emulate the now deprecated
     * RetrieveProperties method.
     *
     * @param listpfs
     * @return list of object content
     * @throws Exception
     */
    List<ObjectContent> retrievePropertiesAllObjects(
            List<PropertyFilterSpec> listpfs) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

        List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();

        RetrieveResult rslts =
                vimPort.retrievePropertiesEx(propCollector, listpfs,
                        propObjectRetrieveOpts);
        if (rslts != null && rslts.getObjects() != null
                && !rslts.getObjects().isEmpty()) {
            listobjcontent.addAll(rslts.getObjects());
        }
        String token = null;
        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }
        while (token != null && !token.isEmpty()) {
            rslts = vimPort.continueRetrievePropertiesEx(propCollector, token);
            token = null;
            if (rslts != null) {
                token = rslts.getToken();
                if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
                    listobjcontent.addAll(rslts.getObjects());
                }
            }
        }

        return listobjcontent;
    }

    void monitorEvents(PropertyFilterSpec spec) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ArrayList<PropertyFilterSpec> listpfs =
                new ArrayList<PropertyFilterSpec>();
        listpfs.add(spec);
        List<ObjectContent> listobjcont = retrievePropertiesAllObjects(listpfs);
        if (listobjcont != null) {
            ObjectContent oc = listobjcont.get(0);
            ArrayOfEvent arrayEvents =
                    (ArrayOfEvent) (oc.getPropSet().get(0)).getVal();

            ArrayList<Event> eventList = (ArrayList<Event>) arrayEvents.getEvent();
            System.out.println("Events In the latestPage are: ");
            for (int i = 0; i < eventList.size(); i++) {
                Event anEvent = eventList.get(i);
                System.out.println("Event: " + anEvent.getClass().getName());
            }
        } else {
            System.out.println("No Events retrieved!");
        }
    }

    @Action
    public void run() throws RuntimeFaultFaultMsg, InvalidStateFaultMsg, InvalidPropertyFaultMsg {
        propCollector = serviceContent.getPropertyCollector();
        initEventManagerRef();
        createEventHistoryCollector();
        PropertyFilterSpec eventFilterSpec = createEventFilterSpec();
        monitorEvents(eventFilterSpec);
    }

}
