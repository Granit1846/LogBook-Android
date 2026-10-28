# Gson uses field names and constructors for saved data and desktop exchange.
-keep class com.example.logbook.data.local.entity.** { *; }
-keep class com.example.logbook.data.model.ResourceItem { *; }
-keep class com.example.logbook.data.model.OilItem { *; }
-keep class com.example.logbook.data.model.AggregateGroup { *; }
-keep class com.example.logbook.data.model.AggregateReplacement { *; }
-keep class com.example.logbook.data.exchange.SyncSession { *; }
-keep class com.example.logbook.data.exchange.SyncHistoryEntry { *; }
-keep class com.example.logbook.data.exchange.SharedProfilePacket { *; }
-keepattributes Signature

# Navigation route names and generated serializers are used at runtime.
-keep class com.example.logbook.ui.navigation.*Route { *; }
