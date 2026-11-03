package com.teletronics.storage;

import org.springframework.boot.SpringApplication;

public class TestStorageApplication {

	static void main(String[] args) {
		SpringApplication.from(StorageApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
