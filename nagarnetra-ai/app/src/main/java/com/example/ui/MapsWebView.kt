package com.example.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.Issue
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapsWebView(
    modifier: Modifier = Modifier,
    mode: String, // "cluster" or "draggable"
    issues: List<Issue> = emptyList(),
    selectedIssue: Issue? = null,
    reportLatitude: Double = 12.9715987,
    reportLongitude: Double = 77.5945627,
    isDarkMode: Boolean = false,
    useSatellite: Boolean = false,
    onLocationUpdated: ((Double, Double) -> Unit)? = null,
    onIssueSelected: ((Issue) -> Unit)? = null
) {
    val defaultLatString = String.format(java.util.Locale.US, "%.7f", reportLatitude)
    val defaultLngString = String.format(java.util.Locale.US, "%.7f", reportLongitude)

    val issuesJsonArray = JSONArray()
    issues.forEach { issue ->
        val obj = JSONObject().apply {
            put("id", issue.id)
            put("title", issue.title)
            put("category", issue.category)
            put("ward", issue.ward)
            put("locationName", issue.locationName)
            put("latitude", issue.latitude)
            put("longitude", issue.longitude)
            put("description", issue.description.replace("\n", " "))
            put("photoBase64", issue.photoBase64 ?: "")
            put("severity", issue.severity)
            put("upvotes", issue.upvotes)
            put("status", issue.status)
            put("slaStatus", issue.slaStatus)
            put("objectId", issue.objectId ?: "")
            put("duplicateReports", issue.duplicateReports)
        }
        issuesJsonArray.put(obj)
    }

    val issuesJsonString = issuesJsonArray.toString()
    val base64IssuesJson = android.util.Base64.encodeToString(
        issuesJsonString.toByteArray(Charsets.UTF_8),
        android.util.Base64.NO_WRAP
    )

    // CSS variables based on mode
    val bgColor = if (isDarkMode) "#0F172A" else "#E8EFF5"
    val popupBg = if (isDarkMode) "#1e293b" else "#ffffff"
    val popupText = if (isDarkMode) "#f8fafc" else "#0f172a"
    val popupSubText = if (isDarkMode) "#94a3b8" else "#475569"
    val attrBg = if (isDarkMode) "rgba(15,23,42,0.85)" else "rgba(255,255,255,0.85)"
    val attrColor = if (isDarkMode) "#94a3b8" else "#475569"
    val tileUrl = if (isDarkMode)
        "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"
    else
        "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png"
    val tileFallback = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    val tileAttrib = "&copy; OpenStreetMap contributors &copy; CARTO"
    val tileFilter = if (isDarkMode) "" else ""

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/MarkerCluster.min.css" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/MarkerCluster.Default.min.css" />
            <style type="text/css">
                * { box-sizing: border-box; }
                html, body {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background-color: $bgColor;
                    overflow: hidden;
                }
                #map {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100vw;
                    height: 100vh;
                    background-color: $bgColor;
                    z-index: 1;
                }
                .leaflet-container {
                    background: $bgColor !important;
                }
                .custom-svg-marker { background: none; border: none; }
                .leaflet-popup-content-wrapper {
                    background: $popupBg;
                    color: $popupText;
                    border-radius: 14px;
                    box-shadow: 0 10px 25px -5px rgba(0,0,0,0.3);
                    font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    border: 1px solid ${if (isDarkMode) "#334155" else "#e2e8f0"};
                }
                .leaflet-popup-tip { background: $popupBg; }
                .leaflet-container a.leaflet-popup-close-button {
                    color: $popupSubText;
                    font-size: 18px;
                    padding: 8px;
                }
                .leaflet-container .leaflet-control-attribution {
                    background: $attrBg !important;
                    color: $attrColor !important;
                    font-size: 9px;
                }
                .leaflet-container .leaflet-control-attribution a {
                    color: ${if (isDarkMode) "#38bdf8" else "#2563eb"} !important;
                }
                .leaflet-control-zoom {
                    margin-top: 65px !important;
                }
                .leaflet-control-zoom a {
                    background-color: $popupBg !important;
                    color: $popupText !important;
                    border: 1px solid ${if (isDarkMode) "#334155" else "#cbd5e1"} !important;
                }
                .leaflet-bar { border: none !important; box-shadow: 0 2px 8px rgba(0,0,0,0.2) !important; }
                .marker-cluster-small, .marker-cluster-medium, .marker-cluster-large {
                    background-color: rgba(255, 107, 0, 0.25) !important;
                }
                .marker-cluster-small div, .marker-cluster-medium div, .marker-cluster-large div {
                    background-color: rgba(255, 107, 0, 0.9) !important;
                    color: white !important;
                    font-weight: bold !important;
                    font-size: 13px !important;
                }
                .pulse-ring {
                    border-radius: 50%;
                    animation: pulse 2s ease-out infinite;
                }
                @keyframes pulse {
                    0% { transform: scale(0.8); opacity: 1; }
                    100% { transform: scale(2.0); opacity: 0; }
                }
                .live-location-icon {
                    background: #3B82F6;
                    width: 14px;
                    height: 14px;
                    border-radius: 50%;
                    border: 2px solid white;
                    box-shadow: 0 0 5px rgba(0,0,0,0.5);
                    position: relative;
                }
                .map-cluster-label { background: transparent; border: 0; }
                .map-cluster-count {
                    width: 36px;
                    height: 36px;
                    border-radius: 50%;
                    background: ${if (isDarkMode) "#1e293b" else "#ffffff"};
                    color: ${if (isDarkMode) "#f8fafc" else "#0f172a"};
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 13px;
                    font-weight: 800;
                    border: 2px solid #B45309;
                    box-shadow: 0 0 0 7px rgba(180,83,9,0.18);
                }
                .live-location-ring {
                    width: 30px;
                    height: 30px;
                    border: 3px solid #3B82F6;
                    border-radius: 50%;
                    position: absolute;
                    top: -10px;
                    left: -10px;
                    animation: pulse 2s ease-out infinite;
                }
            </style>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/leaflet.markercluster.min.js"></script>
        </head>
        <body>

            <div id="map"></div>
            <script>
                var map;
                var markersGroup;
                var clusterCircleGroup;
                var singleMarker;
                var currentMode = "$mode";
                var defaultLat = $defaultLatString;
                var defaultLng = $defaultLngString;
                var isDark = ${if (isDarkMode) "true" else "false"};
                var primaryTile;
                var satelliteTile;
                var currentLayerType = "${if (useSatellite) "satellite" else "standard"}";
                function callAndroid(methodName, arg1, arg2) {
                    try {
                        var bridge = window.AndroidBridge || (typeof AndroidBridge !== 'undefined' ? AndroidBridge : null);
                        if (bridge && bridge[methodName]) {
                            if (arg2 !== undefined) {
                                bridge[methodName](arg1, arg2);
                            } else {
                                bridge[methodName](arg1);
                            }
                        }
                    } catch (e) {
                        console.error("AndroidBridge error:", e);
                    }
                }

                window.onerror = function(message, source, lineno, colno, error) {
                    console.error("JS Error: " + message + " at " + source + ":" + lineno);
                    return true;
                };

                function initMap() {
                    try {
                        var mapDiv = document.getElementById('map');
                        mapDiv.style.width  = window.innerWidth  + 'px';
                        mapDiv.style.height = window.innerHeight + 'px';

                        map = L.map('map', {
                            zoomControl: true,
                            attributionControl: true,
                            fadeAnimation: false,
                            markerZoomAnimation: true,
                            preferCanvas: false
                        }).setView([defaultLat, defaultLng], 15);

                        // Primary tile layer (Carto Voyager = light, Carto Dark = dark)
                        primaryTile = L.tileLayer("$tileUrl", {
                            subdomains: 'abcd',
                            maxZoom: 20,
                            attribution: "$tileAttrib"
                        });

                        satelliteTile = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                            attribution: 'Tiles &copy; Esri',
                            maxZoom: 19
                        });

                        // Fallback to plain OSM
                        var osmFallback = L.tileLayer("$tileFallback", {
                            subdomains: 'abc',
                            maxZoom: 19,
                            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        });

                        var primaryFailed = false;
                        primaryTile.on('tileerror', function() {
                            if (!primaryFailed) {
                                primaryFailed = true;
                                map.removeLayer(primaryTile);
                                if (isDark) {
                                    // fallback OSM with dark filter
                                    osmFallback.addTo(map);
                                    osmFallback.on('tileload', function(e) {
                                        e.tile.style.filter = 'invert(90%) hue-rotate(180deg) brightness(90%) contrast(110%)';
                                    });
                                } else {
                                    osmFallback.addTo(map);
                                }
                            }
                        });

                        if (currentLayerType === "satellite") {
                            satelliteTile.addTo(map);
                        } else {
                            primaryTile.addTo(map);
                        }
                        if (currentMode === "draggable") {
                            setupDraggableMarker();
                        } else {
                            setupClusteredMarkers();
                        }

                        // Resize handling
                        window.addEventListener('resize', function() {
                            var d = document.getElementById('map');
                            d.style.width  = window.innerWidth  + 'px';
                            d.style.height = window.innerHeight + 'px';
                            map.invalidateSize(true);
                        });

                        [0, 100, 300, 600, 1200, 2500].forEach(function(ms) {
                            setTimeout(function() {
                                if (map) map.invalidateSize(true);
                            }, ms);
                        });

                    } catch (err) {
                        console.error("initMap failed: " + err.message);
                    }
                }

                function getSeverityColor(severity) {
                    if (severity >= 9) return "#EF4444";
                    if (severity >= 7) return "#F97316";
                    if (severity >= 5) return "#F59E0B";
                    return "#22C55E";
                }

                function getCategoryColor(category) {
                    var c = (category || '').toLowerCase();
                    if (c.includes("water") || c.includes("lake"))        return "#3B82F6";
                    if (c.includes("light") || c.includes("bescom"))      return "#F59E0B";
                    if (c.includes("garbage") || c.includes("trash"))     return "#F97316";
                    if (c.includes("pothole") || c.includes("road"))      return "#8B5CF6";
                    if (c.includes("sewage") || c.includes("drain"))      return "#B45309";
                    return "#EF4444";
                }

                function getSVGIcon(category, severity, isDraggable) {
                    var color = isDraggable ? "#6366F1" : getCategoryColor(category);
                    var size = isDraggable ? 40 : (severity >= 9 ? 42 : 36);
                    var svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="' + color + '" width="' + size + '" height="' + size + '">'
                        + '<filter id="shadow"><feDropShadow dx="0" dy="2" stdDeviation="2" flood-opacity="0.4"/></filter>'
                        + '<path filter="url(#shadow)" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>'
                        + '</svg>';
                    return L.divIcon({
                        html: svg,
                        className: 'custom-svg-marker',
                        iconSize: [size, size],
                        iconAnchor: [size / 2, size],
                        popupAnchor: [0, -size]
                    });
                }

                function setupDraggableMarker() {
                    singleMarker = L.marker([defaultLat, defaultLng], {
                        draggable: true,
                        icon: getSVGIcon("primary", 5, true)
                    }).addTo(map);

                    map.setView([defaultLat, defaultLng], 15);

                    singleMarker.on('dragend', function() {
                        var pos = singleMarker.getLatLng();
                        callAndroid('onLocationUpdated', pos.lat, pos.lng);
                    });

                    map.on('click', function(e) {
                        singleMarker.setLatLng(e.latlng);
                        callAndroid('onLocationUpdated', e.latlng.lat, e.latlng.lng);
                    });
                }

                function escapeHtml(value) {
                    return String(value == null ? '' : value)
                        .replace(/&/g, '&amp;')
                        .replace(/</g, '&lt;')
                        .replace(/>/g, '&gt;')
                        .replace(/"/g, '&quot;')
                        .replace(/'/g, '&#39;');
                }

                function escapeJsArg(value) {
                    return String(value == null ? '' : value)
                        .replace(/\\/g, '\\\\')
                        .replace(/'/g, "\\'")
                        .replace(/\r?\n/g, ' ');
                }


                function getStatusBadgeColor(status) {
                    if (!status) return "#64748B";
                    var s = status.toLowerCase();
                    if (s.includes("resolved"))   return "#22C55E";
                    if (s.includes("progress"))   return "#3B82F6";
                    if (s.includes("urgent") || s.includes("escalat")) return "#EF4444";
                    if (s.includes("verified"))   return "#6366F1";
                    return "#F59E0B";
                }


                function distanceMeters(a, b) {
                    var earth = 6371000;
                    var dLat = (b.latitude - a.latitude) * Math.PI / 180;
                    var dLng = (b.longitude - a.longitude) * Math.PI / 180;
                    var lat1 = a.latitude * Math.PI / 180;
                    var lat2 = b.latitude * Math.PI / 180;
                    var h = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
                    return 2 * earth * Math.asin(Math.sqrt(h));
                }

                function calculateClusterPriority(issue) {
                    var duplicateBoost = Math.min(24, Math.max(0, (issue.duplicateReports || 1) - 1) * 6);
                    var upvoteBoost = Math.min(22, (issue.upvotes || 0) * 2);
                    return Math.min(100, Math.round((issue.severity || 5) * 7 + upvoteBoost + duplicateBoost));
                }

                function buildIssueClusters(issuesList) {
                    var clusters = [];
                    issuesList.forEach(function(issue) {
                        var existing = clusters.find(function(cluster) {
                            var sameCategory = cluster.category === issue.category;
                            var sameObject = cluster.objectId && issue.objectId && cluster.objectId.toUpperCase() === issue.objectId.toUpperCase();
                            return (sameCategory || sameObject) && distanceMeters(cluster, issue) <= 260;
                        });
                        if (existing) {
                            existing.issues.push(issue);
                            existing.count += Math.max(1, issue.duplicateReports || 1);
                            existing.latitude = existing.issues.reduce(function(sum, i) { return sum + i.latitude; }, 0) / existing.issues.length;
                            existing.longitude = existing.issues.reduce(function(sum, i) { return sum + i.longitude; }, 0) / existing.issues.length;
                            existing.priority = Math.max(existing.priority, calculateClusterPriority(issue));
                        } else {
                            clusters.push({
                                latitude: issue.latitude,
                                longitude: issue.longitude,
                                category: issue.category,
                                locationName: issue.locationName,
                                objectId: issue.objectId || '',
                                issues: [issue],
                                count: Math.max(1, issue.duplicateReports || 1),
                                priority: calculateClusterPriority(issue)
                            });
                        }
                    });
                    return clusters.filter(function(cluster) { return cluster.count > 1 || cluster.issues.length > 1; });
                }

                function drawClusterCircles(issuesList) {
                    if (clusterCircleGroup) clusterCircleGroup.clearLayers();
                    else clusterCircleGroup = L.layerGroup().addTo(map);
                    buildIssueClusters(issuesList).forEach(function(cluster) {
                        var color = getCategoryColor(cluster.category);
                        var radius = Math.min(58, 24 + cluster.count * 5);
                        var circle = L.circleMarker([cluster.latitude, cluster.longitude], {
                            radius: radius,
                            color: color,
                            weight: 2,
                            fillColor: color,
                            fillOpacity: isDark ? 0.28 : 0.20,
                            opacity: 0.85
                        });
                        var label = L.divIcon({
                            html: '<div class="map-cluster-count" style="border-color:' + color + ';box-shadow:0 0 0 7px ' + color + '22;">' + cluster.count + '</div>',
                            className: 'map-cluster-label',
                            iconSize: [36, 36],
                            iconAnchor: [18, 18]
                        });
                        var marker = L.marker([cluster.latitude, cluster.longitude], { icon: label, interactive: false });
                        circle.bindPopup('<div style="font-family:system-ui,sans-serif;padding:6px;width:210px;"><h4 style="margin:0 0 6px 0;font-size:13px;">' + cluster.count + ' grouped ' + escapeHtml(cluster.category) + ' reports</h4><p style="margin:0 0 6px 0;font-size:11px;color:#64748b;">' + escapeHtml(cluster.locationName) + '</p><p style="margin:0;font-size:11px;color:#64748b;">Priority ' + cluster.priority + '/100</p></div>');
                        clusterCircleGroup.addLayer(circle);
                        clusterCircleGroup.addLayer(marker);
                    });
                }

                function setupClusteredMarkers() {
                    var issuesList = [];
                    try {
                        var b64 = "$base64IssuesJson".trim().replace(/\s/g, '');
                        issuesList = JSON.parse(decodeURIComponent(escape(atob(b64))));
                    } catch (e) {
                        console.error("JSON parse error: " + e.message);
                    }

                    markersGroup = L.markerClusterGroup({
                        maxClusterRadius: 60,
                        showCoverageOnHover: false,
                        spiderfyOnMaxZoom: true
                    });
                    drawClusterCircles(issuesList);

                    var popupBgColor = isDark ? "#1e293b" : "#ffffff";
                    var popupTextColor = isDark ? "#f8fafc" : "#0f172a";
                    var popupSubColor = isDark ? "#94a3b8" : "#475569";
                    var popupBorderColor = isDark ? "#334155" : "#e2e8f0";

                    issuesList.forEach(function(issue) {
                        var marker = L.marker([issue.latitude, issue.longitude], {
                            icon: getSVGIcon(issue.category, issue.severity || 5, false)
                        });

                        var imgTag = "";
                        if (issue.photoBase64 && issue.photoBase64.length > 20) {
                            imgTag = '<img src="data:image/jpeg;base64,' + issue.photoBase64 + '" style="width:100%;height:auto;max-height:110px;border-radius:10px;margin:8px 0;object-fit:cover;display:block;" />';
                        }

                        var statusColor = getStatusBadgeColor(issue.status);
                        var catColor = getCategoryColor(issue.category);

                        var infoHtml = '<div style="color:' + popupTextColor + ';font-family:system-ui,sans-serif;padding:6px;width:210px;">'
                            + '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">'
                            + '<span style="font-size:9px;background:' + catColor + '22;color:' + catColor + ';padding:3px 9px;border-radius:12px;font-weight:bold;text-transform:uppercase;border:1px solid ' + catColor + '44;">' + escapeHtml(issue.category || 'Issue') + '</span>'
                            + '<span style="font-size:9px;background:' + statusColor + '22;color:' + statusColor + ';padding:2px 7px;border-radius:10px;font-weight:bold;">' + escapeHtml(issue.status || '') + '</span>'
                            + '</div>'
                            + '<h4 style="margin:0 0 3px 0;font-size:13px;font-weight:bold;color:' + popupTextColor + ';line-height:1.3;">' + escapeHtml(issue.title) + '</h4>'
                            + '<p style="margin:0 0 5px 0;font-size:10px;color:' + popupSubColor + ';">📍 ' + escapeHtml(issue.locationName) + '</p>'
                            + imgTag
                            + '<p style="margin:0 0 8px 0;font-size:11px;color:' + popupSubColor + ';line-height:1.5;">' + escapeHtml((issue.description || '').substring(0, 120)) + (issue.description && issue.description.length > 120 ? '...' : '') + '</p>'
                            + '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">'
                            + '<span style="font-size:10px;color:#FF6B00;font-weight:bold;">👥 ' + (issue.upvotes || 0) + ' citizens</span>'
                            + '<span style="font-size:10px;color:' + (issue.severity >= 8 ? '#EF4444' : issue.severity >= 6 ? '#F97316' : '#22C55E') + ';font-weight:bold;">⚡ Sev: ' + (issue.severity || 5) + '/10</span>'
                            + '</div>'
                            + '<button onclick="callAndroid(\'onMarkerSelected\',\'' + escapeJsArg(issue.id) + '\')" style="width:100%;height:34px;background:linear-gradient(135deg,#FF6B00,#FF8C42);color:white;border:none;border-radius:8px;font-size:12px;font-weight:bold;cursor:pointer;letter-spacing:0.5px;">VIEW DETAILS →</button>'
                            + '</div>';

                        marker.bindPopup(infoHtml, { maxWidth: 230, className: '' });
                        markersGroup.addLayer(marker);
                    });

                    map.addLayer(markersGroup);

                    // Add live location pulsing blue dot marker
                    try {
                        var liveIcon = L.divIcon({
                            html: '<div class="live-location-icon"><div class="live-location-ring"></div></div>',
                            className: 'custom-svg-marker',
                            iconSize: [14, 14],
                            iconAnchor: [7, 7]
                        });
                        L.marker([defaultLat, defaultLng], { icon: liveIcon })
                            .addTo(map)
                            .bindPopup('<b style="color:' + popupTextColor + ';">Your Live Location</b>', { closeButton: false });
                    } catch (e) {
                        console.error("Error placing live location marker: " + e.message);
                    }
                }

                function updateMarkerPosition(lat, lng) {
                    if (singleMarker) {
                        singleMarker.setLatLng([lat, lng]);
                        map.panTo([lat, lng]);
                    }
                    if (map) map.invalidateSize(true);
                }

                function updateIssues(newBase64) {
                    try {
                        var b64 = newBase64.trim().replace(/\s/g, '');
                        var newIssues = JSON.parse(decodeURIComponent(escape(atob(b64))));
                        
                        if (markersGroup) {
                            markersGroup.clearLayers();
                        }
                        drawClusterCircles(newIssues);
                        if (!markersGroup) {
                            markersGroup = L.markerClusterGroup({
                                maxClusterRadius: 60,
                                showCoverageOnHover: false,
                                spiderfyOnMaxZoom: true
                            });
                        }
                        
                        var popupBgColor = isDark ? "#1e293b" : "#ffffff";
                        var popupTextColor = isDark ? "#f8fafc" : "#0f172a";
                        var popupSubColor = isDark ? "#94a3b8" : "#475569";
                        
                        newIssues.forEach(function(issue) {
                            var marker = L.marker([issue.latitude, issue.longitude], {
                                icon: getSVGIcon(issue.category, issue.severity || 5, false)
                            });
                            
                            var imgTag = "";
                            if (issue.photoBase64 && issue.photoBase64.length > 20) {
                                imgTag = '<img src="data:image/jpeg;base64,' + issue.photoBase64 + '" style="width:100%;height:auto;max-height:110px;border-radius:10px;margin:8px 0;object-fit:cover;display:block;" />';
                            }
                            
                            var statusColor = getStatusBadgeColor(issue.status);
                            var catColor = getCategoryColor(issue.category);
                            
                            var infoHtml = '<div style="color:' + popupTextColor + ';font-family:system-ui,sans-serif;padding:6px;width:210px;">'
                                + '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">'
                                + '<span style="font-size:9px;background:' + catColor + '22;color:' + catColor + ';padding:3px 9px;border-radius:12px;font-weight:bold;text-transform:uppercase;border:1px solid ' + catColor + '44;">' + escapeHtml(issue.category || 'Issue') + '</span>'
                                + '<span style="font-size:9px;background:' + statusColor + '22;color:' + statusColor + ';padding:2px 7px;border-radius:10px;font-weight:bold;">' + escapeHtml(issue.status || '') + '</span>'
                                + '</div>'
                                + '<h4 style="margin:0 0 3px 0;font-size:13px;font-weight:bold;color:' + popupTextColor + ';line-height:1.3;">' + escapeHtml(issue.title) + '</h4>'
                                + '<p style="margin:0 0 5px 0;font-size:10px;color:' + popupSubColor + ';">📍 ' + escapeHtml(issue.locationName) + '</p>'
                                + imgTag
                                + '<p style="margin:0 0 8px 0;font-size:11px;color:' + popupSubColor + ';line-height:1.5;">' + escapeHtml((issue.description || '').substring(0, 120)) + (issue.description && issue.description.length > 120 ? '...' : '') + '</p>'
                                + '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">'
                                + '<span style="font-size:10px;color:#FF6B00;font-weight:bold;">👥 ' + (issue.upvotes || 0) + ' citizens</span>'
                                + '<span style="font-size:10px;color:' + (issue.severity >= 8 ? '#EF4444' : issue.severity >= 6 ? '#F97316' : '#22C55E') + ';font-weight:bold;">⚡ Sev: ' + (issue.severity || 5) + '/10</span>'
                                + '</div>'
                                + '<button onclick="callAndroid(\'onMarkerSelected\',\'' + escapeJsArg(issue.id) + '\')" style="width:100%;height:34px;background:linear-gradient(135deg,#FF6B00,#FF8C42);color:white;border:none;border-radius:8px;font-size:12px;font-weight:bold;cursor:pointer;letter-spacing:0.5px;">VIEW DETAILS →</button>'
                                + '</div>';
                                
                            marker.bindPopup(infoHtml, { maxWidth: 230, className: '' });
                            markersGroup.addLayer(marker);
                        });
                        
                        if (map && !map.hasLayer(markersGroup)) {
                            map.addLayer(markersGroup);
                        }
                    } catch (e) {
                        console.error("updateIssues failed: " + e.message);
                    }
                }

                window.onload = function() {
                    setTimeout(initMap, 50);
                };
            </script>
        </body>
        </html>
    """.trimIndent()

    androidx.compose.runtime.key(isDarkMode, mode, useSatellite) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadsImagesAutomatically = true
                        blockNetworkImage = false
                        blockNetworkLoads = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36 NagarNetra/1.0"
                    }

                    WebView.setWebContentsDebuggingEnabled(true)

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                            android.util.Log.d("LeafletWebView", "${msg?.messageLevel()}: ${msg?.message()} (line ${msg?.lineNumber()})")
                            return true
                        }
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: android.webkit.GeolocationPermissions.Callback?
                        ) {
                            callback?.invoke(origin, true, false)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.postDelayed({
                                view.evaluateJavascript("if(typeof map !== 'undefined' && map) { map.invalidateSize(true); }", null)
                            }, 300)
                            view?.postDelayed({
                                view.evaluateJavascript("if(typeof map !== 'undefined' && map) { map.invalidateSize(true); }", null)
                            }, 1200)
                            
                            // Load initial issues after page load finish
                            view?.evaluateJavascript("if(typeof updateIssues === 'function') { updateIssues('$base64IssuesJson'); }", null)

                            // Center selected issue if present
                            if (mode == "cluster" && selectedIssue != null) {
                                val lat = String.format(java.util.Locale.US, "%.7f", selectedIssue.latitude)
                                val lng = String.format(java.util.Locale.US, "%.7f", selectedIssue.longitude)
                                view?.evaluateJavascript("if(typeof map !== 'undefined' && map) { map.setView([$lat, $lng], 15); }", null)
                            }
                        }
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onLocationUpdated(lat: Double, lng: Double) {
                            onLocationUpdated?.invoke(lat, lng)
                        }

                        @JavascriptInterface
                        fun onMarkerSelected(id: String) {
                            issues.find { it.id == id }?.let { onIssueSelected?.invoke(it) }
                        }

                        @JavascriptInterface
                        fun logError(msg: String) {
                            android.util.Log.e("LeafletJS", msg)
                        }
                    }, "AndroidBridge")

                    loadDataWithBaseURL(
                        "https://openstreetmap.org",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { webView ->
                if (mode == "draggable") {
                    val lat = String.format(java.util.Locale.US, "%.7f", reportLatitude)
                    val lng = String.format(java.util.Locale.US, "%.7f", reportLongitude)
                    webView.evaluateJavascript("updateMarkerPosition($lat, $lng)", null)
                } else if (mode == "cluster") {
                    webView.evaluateJavascript("if(typeof updateIssues === 'function') { updateIssues('$base64IssuesJson'); }", null)
                    if (selectedIssue != null) {
                        val lat = String.format(java.util.Locale.US, "%.7f", selectedIssue.latitude)
                        val lng = String.format(java.util.Locale.US, "%.7f", selectedIssue.longitude)
                        webView.evaluateJavascript("if(typeof map !== 'undefined' && map) { map.setView([$lat, $lng], 15); }", null)
                    }
                }
            }
        )
    }
}