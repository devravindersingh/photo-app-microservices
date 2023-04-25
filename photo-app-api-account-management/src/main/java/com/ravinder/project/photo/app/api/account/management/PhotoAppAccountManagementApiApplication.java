package com.ravinder.project.photo.app.api.account.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PhotoAppAccountManagementApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhotoAppAccountManagementApiApplication.class, args);
	}

}
