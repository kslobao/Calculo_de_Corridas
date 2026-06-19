package com.calculocorridas.domain.entities

enum class AppSource(val packageName: String, val displayName: String, val key: String) {
    UBER("com.ubercab.driver", "Uber", "uber"),
    NINETY_NINE("com.taxis99.driver", "99", "99"),
    INDRIVE("sinet.startup.inDriver", "inDrive", "indrive"),
    IFOOD("br.com.ifood.driver", "iFood", "ifood");

    companion object {
        fun fromPackage(packageName: String): AppSource? =
            entries.find { it.packageName == packageName }

        fun fromKey(key: String): AppSource? =
            entries.find { it.key == key }
    }
}
