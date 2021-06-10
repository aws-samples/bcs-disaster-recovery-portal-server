// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.aws.service;

import aws.proserve.bcs.dr.aws.AwsInstanceType;
import aws.proserve.bcs.dr.aws.AwsSecurityGroup;
import aws.proserve.bcs.dr.aws.ImmutableAwsInstanceType;
import aws.proserve.bcs.dr.project.Region;
import aws.proserve.bcs.dr.vpc.Filters;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.InstanceTypeInfo;
import com.amazonaws.services.ec2.model.SecurityGroup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class AwsService {

    public Region[] getRegions() {
        return Stream.of(Regions.values())
                .map(Region::new)
                .sorted(Comparator.comparing(Region::getName))
                .toArray(Region[]::new);
    }

    public AwsInstanceType[] getInstanceTypes(String region) {
        final var ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(region)
                .build();

        final var instanceTypes = new ArrayList<InstanceTypeInfo>();
        final var describeRequest = new DescribeInstanceTypesRequest();
        DescribeInstanceTypesResult result;
        do {
            result = ec2.describeInstanceTypes(describeRequest);
            describeRequest.setNextToken(result.getNextToken());
            instanceTypes.addAll(result.getInstanceTypes());
        } while (result.getNextToken() != null);

        return instanceTypes.stream()
                .map(t -> ImmutableAwsInstanceType.builder()
                        .id(t.getInstanceType())
                        .name(t.getInstanceType())
                        .build())
                .toArray(AwsInstanceType[]::new);
    }

    public AwsSecurityGroup[] getSecurityGroups(String region, String vpcId) {
        final var ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(region)
                .build();

        final var groups = new ArrayList<SecurityGroup>();
        final var describeRequest = new DescribeSecurityGroupsRequest().withFilters(Filters.vpcId(vpcId));
        DescribeSecurityGroupsResult result;
        do {
            result = ec2.describeSecurityGroups(describeRequest);
            describeRequest.setNextToken(result.getNextToken());
            groups.addAll(result.getSecurityGroups());
        } while (result.getNextToken() != null);

        return groups.stream()
                .map(t -> new AwsSecurityGroup(t.getGroupId(), t.getGroupName()))
                .toArray(AwsSecurityGroup[]::new);
    }
}
