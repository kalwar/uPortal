/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.portal.portlets.statistics;

import com.google.common.collect.ImmutableList;
import com.google.visualization.datasource.base.TypeMismatchException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.value.NumberValue;
import com.google.visualization.datasource.datatable.value.Value;
import com.google.visualization.datasource.datatable.value.ValueType;
import org.jasig.portal.events.aggr.AggregationInterval;
import org.jasig.portal.events.aggr.BaseAggregationDao;
import org.jasig.portal.events.aggr.BaseAggregationDateTimeComparator;
import org.jasig.portal.events.aggr.groups.AggregatedGroupLookupDao;
import org.jasig.portal.events.aggr.groups.AggregatedGroupMapping;
import org.jasig.portal.events.aggr.login.LoginAggregation;
import org.jasig.portal.events.aggr.login.LoginAggregationDao;
import org.jasig.portal.events.aggr.login.LoginAggregationDiscriminator;
import org.jasig.portal.events.aggr.login.LoginAggregationDiscriminatorImpl;
import org.jasig.portal.events.aggr.login.LoginAggregationKey;
import org.jasig.portal.events.aggr.login.LoginAggregationKeyImpl;
import org.jasig.portal.utils.ComparableExtractingComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Login reports
 * 
 * @author Jen Bourey, jennifer.bourey@gmail.com
 */
@Controller
@RequestMapping(value="VIEW")
public class LoginTotalsStatisticsController extends
        BaseStatisticsReportController<LoginAggregation, LoginAggregationKey,
                LoginAggregationDiscriminator, LoginReportForm> {
    private static final String DATA_TABLE_RESOURCE_ID = "loginData";
    private final static String REPORT_NAME = "login.totals";

    @Autowired
    private LoginAggregationDao<LoginAggregation> loginDao;

    @Autowired
    private AggregatedGroupLookupDao aggregatedGroupDao;

    @RenderMapping(value="MAXIMIZED", params="report=" + REPORT_NAME)
    public String getLoginView() throws TypeMismatchException {
        return "jsp/Statistics/reportGraph";
    }
    
    @ResourceMapping(DATA_TABLE_RESOURCE_ID)
    public ModelAndView renderLoginAggregationReport(LoginReportForm form) throws TypeMismatchException {
        return renderAggregationReport(form);
    }
    
    @Override
    protected LoginReportForm createReportFormRequest() {
        return new LoginReportForm();
    }

    @Override
    public String getReportName() {
        return REPORT_NAME;
    }

    @Override
    public String getReportDataResourceId() {
        return DATA_TABLE_RESOURCE_ID;
    }

    @Override
    protected BaseAggregationDao<LoginAggregation, LoginAggregationKey> getBaseAggregationDao() {
        return this.loginDao;
    }

    @Override
    protected Set<LoginAggregationKey> createAggregationsQueryKeyset(
            Set<LoginAggregationDiscriminator> discriminators, LoginReportForm form) {
        AggregatedGroupMapping groupToUse = discriminators.iterator().next().getAggregatedGroup();
        final AggregationInterval interval = form.getInterval();
        final HashSet<LoginAggregationKey> keys = new HashSet<LoginAggregationKey>();
        keys.add(new LoginAggregationKeyImpl(interval, groupToUse));
        return keys;
    }

    @Override
    protected ComparableExtractingComparator<?, ?> getDiscriminatorComparator() {
        return LoginAggregationDiscriminatorImpl.Comparator.INSTANCE;
    }

    @Override
//    protected Map<LoginAggregationDiscriminator, SortedSet<LoginAggregation>>
//            createColumnDiscriminatorMap(LoginReportForm form) {
//        return getDefaultGroupedColumnDiscriminatorMap(form);
//    }
    /**
     * Default implementation to create a map of the report column discriminators based on the submitted form to
     * collate the aggregation data into each column of a report.
     * The map entries are a time-ordered sorted set of aggregation data points.
     * Subclasses may override this method to obtain more from the form than just AggregatedGroupMappings as
     * report columns.
     *
     * @param form Form submitted by the user
     * @return Map of report column discriminators to sorted set of time-based aggregation data
     */
    //todo need to throw in a factory method to avoid duplicating this code.
    protected Map<LoginAggregationDiscriminator, SortedSet<LoginAggregation>>
    createColumnDiscriminatorMap (LoginReportForm form){
        List<Long> groups = form.getGroups();
        //Collections used to track the queried groups and the results
        final Map<LoginAggregationDiscriminator, SortedSet<LoginAggregation>> groupedAggregations =
                new TreeMap<LoginAggregationDiscriminator, SortedSet<LoginAggregation>>(LoginAggregationDiscriminatorImpl.Comparator.INSTANCE);

        //Get concrete group mapping objects that are being queried for
        for (final Long queryGroupId : groups) {
            final LoginAggregationDiscriminator groupMapping =
                    new LoginAggregationDiscriminatorImpl(this.aggregatedGroupDao.getGroupMapping(queryGroupId));

            //Create the set the aggregations for this report column will be stored in, sorted chronologically
            final SortedSet<LoginAggregation> aggregations = new TreeSet<LoginAggregation>(BaseAggregationDateTimeComparator.INSTANCE);

            //Map the group to the set
            groupedAggregations.put(groupMapping, aggregations);
        }

        return groupedAggregations;
    }


    @Override
    protected List<ColumnDescription> getColumnDescriptions(LoginAggregationDiscriminator columnDiscriminator, LoginReportForm form) {
        final String groupName = columnDiscriminator.getAggregatedGroup().getGroupName();
        
        if (form.isTotalLogins() && form.isUniqueLogins()) {
            return ImmutableList.of(
                    //THE ORDER OF RETURNED COLUMNS HERE MUST MATCH THE ORDER OF THE VALUES RETURNED IN createRowValues
                    new ColumnDescription(groupName + "-uniqueLogins", ValueType.NUMBER, groupName + " - Unique Logins"),
                    new ColumnDescription(groupName + "-totalLogins", ValueType.NUMBER, groupName + " - Total Logins")
                );
        }
        else if (form.isUniqueLogins()) {
            return Collections.singletonList(new ColumnDescription(groupName + "-uniqueLogins", ValueType.NUMBER, groupName + " - Unique Logins"));
        }
        else {
            return Collections.singletonList(new ColumnDescription(groupName + "-totalLogins", ValueType.NUMBER, groupName + " - Total Logins"));
        }
    }

    @Override
    protected List<Value> createRowValues(LoginAggregation aggr, LoginReportForm form) {
        final int loginCount;
        final int uniqueLoginCount;
        if (aggr == null) {
            loginCount = 0;
            uniqueLoginCount = 0;
        }
        else {
            loginCount = aggr.getLoginCount();
            uniqueLoginCount = aggr.getUniqueLoginCount();
        }
        
        if (form.isTotalLogins() && form.isUniqueLogins()) {
            return ImmutableList.<Value>of(
                    //THE ORDER OF RETURNED VALUES HERE MUST MATCH THE ORDER OF THE COLUMNS RETURNED IN getColumnDescriptions
                    new NumberValue(uniqueLoginCount),
                    new NumberValue(loginCount)
                );
        }
        else if (form.isUniqueLogins()) {
            return Collections.<Value>singletonList(new NumberValue(uniqueLoginCount));
        }
        else {
            return Collections.<Value>singletonList(new NumberValue(loginCount));
        }
    }
}
