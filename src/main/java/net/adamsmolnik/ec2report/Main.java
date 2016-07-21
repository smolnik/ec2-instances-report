package net.adamsmolnik.ec2report;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;

/**
 * @author Adam
 *
 */
public class Main {

	public static void main(String[] args) {
		AtomicInteger count = new AtomicInteger();
		Stream.of(Regions.values()).filter(r -> r != Regions.GovCloud && r != Regions.CN_NORTH_1).forEach(r -> {
			System.out.println(r);
			DescribeInstancesRequest req = new DescribeInstancesRequest();
			AmazonEC2 ec2 = new AmazonEC2Client().withRegion(r);
			DescribeInstancesResult res = ec2.describeInstances(req);
			res.getReservations().stream().flatMap(reservation -> reservation.getInstances().stream())
					.sorted((i1, i2) -> i1.getState().getName().compareTo(i2.getState().getName())).forEach(instance -> {
				print(r, instance, count);
			});
		});
	}

	private static void print(Regions r, Instance instance, AtomicInteger count) {
		// =SPLIT to copy and paste into Google Spreadsheets
		if ("running".equals(instance.getState().getName())) {
			String az = instance.getPlacement().getAvailabilityZone();
			String id = instance.getInstanceId();
			String type = instance.getInstanceType();
			String name = getName(instance.getTags());
			String publicIp = instance.getPublicIpAddress() == null ? " " : instance.getPublicIpAddress();
			String tags = getTags(instance.getTags());
			String line = count.incrementAndGet() + ".;" + az + ";" + id + ";" + type + ";" + name + ";" + publicIp + ";" + tags;
			System.out.println("=SPLIT(\"" + line + "\", \";\")");
		}

	}

	private static String getTags(List<Tag> tags) {
		StringBuilder sb = new StringBuilder();
		tags.stream().filter(t -> !"Name".equals(t.getKey())).forEach(t -> {
			sb.append(t.getKey()).append("=").append(t.getValue()).append(",");
		});
		return sb.toString();
	}

	private static String getName(List<Tag> tags) {
		return tags.stream().filter(t -> "Name".equals(t.getKey())).findFirst().orElse(new Tag("", " ")).getValue();
	}

}
