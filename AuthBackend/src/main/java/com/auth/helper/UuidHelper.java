package com.auth.helper;

import java.util.UUID;

public class UuidHelper {
	
	public static UUID parseUuid(String uuid) {
		return UUID.fromString(uuid);
	}

}
