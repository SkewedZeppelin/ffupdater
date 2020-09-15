package de.marmaro.krt.ffupdater.version;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;

import de.marmaro.krt.ffupdater.ApkMirrorHelper;
import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

import static org.exparity.hamcrest.date.LocalDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by Tobiwan on 13.05.2020.
 */
public class FocusIT {

    static ApkMirrorHelper.RssFeedResponse rssFeedResponse;

    @BeforeClass
    public static void setUp() throws ParserConfigurationException, SAXException, IOException {
        rssFeedResponse = ApkMirrorHelper.getRssFeedResponse("https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/feed/");
    }

    @Test
    public void verify_focus_arm() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void verify_focus_aarch64() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.AARCH64);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_focus_x86_shouldFail() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_focus_x8664_shouldFail() throws IOException {
        verify(App.FIREFOX_FOCUS, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void verify_klar_arm() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.ARM);
    }

    @Test
    public void verify_klar_aarch64() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_klar_x86_shouldFail() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_klar_x8664_shouldFail() throws IOException {
        verify(App.FIREFOX_KLAR, DeviceEnvironment.ABI.X86_64);
    }

    @Test
    public void is_focus_aarch64_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox-focus-private-browser", "firefox-focus-the-privacy-browser");
        replacements.put("release", "android-apk-download");

        check_focus_is_up_to_date(replacements, DeviceEnvironment.ABI.AARCH64);
    }

    @Test
    public void is_focus_arm_up_to_date() throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("firefox-focus-private-browser", "firefox-focus-the-privacy-browser");
        replacements.put("release", "2-android-apk-download");

        check_focus_is_up_to_date(replacements, DeviceEnvironment.ABI.ARM);
    }

    private static void check_focus_is_up_to_date(Map<String, String> replacements, DeviceEnvironment.ABI abi) throws ParserConfigurationException, SAXException, IOException {
        final Focus focus = Focus.findLatest(App.FIREFOX_FOCUS, abi);
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(focus.getTimestamp()));
        final Duration ageOfRelease = Duration.between(timestamp, LocalDateTime.now(ZoneOffset.UTC));

        if (ageOfRelease.isNegative()) {
            fail("the age of the app release on Mozilla CI can never be negative");
        } else if (ageOfRelease.toHours() < 48) {
            // if the app is pretty new (app release was in the last 48 hours) then a different hash value is possible
            String hashFromApkMirror = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(rssFeedResponse, replacements);
            if (!Objects.equals(hashFromApkMirror, focus.getHash().toString())) {
                String format = "%s (ignore this error because the latest release on Mozilla CI is only %d hours old and APK Mirror is not so fast) " +
                        "hashes are different but skip - expected: %s, but was: %s\n";
                System.err.printf(format, App.FIREFOX_FOCUS, ageOfRelease.toHours(), hashFromApkMirror, focus.getHash().toString());
            }
        } else if (ageOfRelease.toDays() < 21) {
            // the app is not new - the hashes must be equal
            String hashFromApkMirror = ApkMirrorHelper.extractSha256HashFromAbiVersionPage(rssFeedResponse, replacements);
            assertEquals(hashFromApkMirror, focus.getHash().toString());
        } else {
            fail("the app from Mozilla CI is too old");
        }
    }

    @Test
    public void is_klar_up_to_date() {
        final Focus focus = Focus.findLatest(App.FIREFOX_KLAR, DeviceEnvironment.ABI.AARCH64);
        final String timestampString = focus.getTimestamp();
        final LocalDateTime timestamp = LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampString));
        final LocalDateTime expectedRelease = rssFeedResponse.getPubDate();

        // for releases which are only release on the Mozilla CI and not on APKMirror
        if (timestamp.isAfter(expectedRelease)) {
            // max 1 week difference
            assertThat(timestamp, within(7, ChronoUnit.DAYS, expectedRelease));
            System.out.println("Mozialla CI offers a non released version of FIREFOX_KLAR");
            return;
        }

        assertThat(timestamp, within(24, ChronoUnit.HOURS, expectedRelease));
    }

    private static void verify(App app, DeviceEnvironment.ABI abi) throws IOException {
        final Focus focus = Focus.findLatest(app, abi);
        final String downloadUrl = focus.getDownloadUrl();
        final String timestamp = focus.getTimestamp();
        assertThat(String.format("download url of %s with %s is empty", app, abi), downloadUrl, is(not(emptyString())));
        assertThat(String.format("timestamp of %s with %s is empty", app, abi), timestamp, is(not(emptyString())));

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime parsedTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        final long daysOld = ChronoUnit.DAYS.between(now, parsedTimestamp);
        assertThat(String.format("timestamp of %s with %s is too old", app, abi), daysOld, lessThan(31L));

        // check if downloadUrl is valid
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(downloadUrl).openConnection();
        urlConnection.setRequestMethod("HEAD");
        try {
            urlConnection.getInputStream();
        } finally {
            urlConnection.disconnect();
        }
        System.out.printf("%s (%s) - downloadUrl: %s timestamp: %s\n", app, abi, downloadUrl, timestamp);
    }
}