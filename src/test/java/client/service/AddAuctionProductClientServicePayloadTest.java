package client.service;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.model.RegularUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddAuctionProductClientServicePayloadTest {
    private final AddAuctionProductClientService service = new AddAuctionProductClientService();
    private final RegularUser user = new RegularUser("U1", "seller", "pw", "seller@example.com");

    @TempDir
    Path tempDir;

    @Test
    void buildPayloadUploadsImageAndBuildsElectronicsFields() throws Exception {
        Path image = imageFile("phone.jpg");
        FakeMessageTransport transport = uploadTransport("IMG1");

        Map<String, Object> payload = service.buildPayload(
                transport, user, " Phone ", " Desc ", "Do dien tu", List.of(image.toString()), fields(Map.of(
                        "#elecStartPriceField", "50000",
                        "#elecBrandField", "Samsung",
                        "#elecWarrantyField", "12 months"
                ), 2, 4));

        assertEquals("Phone", payload.get("name"));
        assertEquals("Desc", payload.get("description"));
        assertEquals("seller", payload.get("sellerId"));
        assertEquals("ELECTRONICS", payload.get("type"));
        assertEquals("Samsung", payload.get("brand"));
        assertEquals("12 months", payload.get("warrantyPeriod"));
        assertEquals(List.of("IMG1"), payload.get("images"));
        assertEquals(2880L, payload.get("duration"));

        Message upload = transport.lastSent();
        assertEquals(MessageType.UPLOAD_IMAGE, upload.getType());
        Map<?, ?> uploadData = assertInstanceOf(Map.class, upload.getData());
        assertEquals("phone.jpg", uploadData.get("filename"));
        assertArrayEquals(Files.readAllBytes(image), (byte[]) uploadData.get("imageBytes"));
    }

    @Test
    void buildPayloadCoversCategorySpecificFields() throws Exception {
        Path image = imageFile("item.bin");

        Map<String, Object> fashion = service.buildPayload(uploadTransport("F1"), user, "Fashion", "Desc", "Thoi trang",
                List.of(image.toString()), fields(Map.of(
                        "#fashionStartPriceField", "10000",
                        "#fashionBrandField", "Brand",
                        "#fashionMaterialField", "Cotton"
                ), 2, 4));
        assertEquals("FASHION", fashion.get("type"));
        assertEquals("Brand", fashion.get("brand"));
        assertEquals("Cotton", fashion.get("material"));

        Map<String, Object> jewelry = service.buildPayload(uploadTransport("J1"), user, "Jewelry", "Desc", "Trang suc",
                List.of(image.toString()), fields(Map.of(
                        "#jewelryStartPriceField", "25000",
                        "#jewelryMaterialField", "Gold",
                        "#jewelryWeightField", "1.5"
                ), 2, 4));
        assertEquals("JEWELRY", jewelry.get("type"));
        assertEquals("Gold", jewelry.get("material"));
        assertEquals(1.5, (double) jewelry.get("weight"), 0.001);

        Map<String, Object> art = service.buildPayload(uploadTransport("A1"), user, "Art", "Desc", "Suu tam",
                List.of(image.toString()), fields(Map.of(
                        "#artStartPriceField", "100000",
                        "#artCreatorField", "Artist",
                        "#artMaterialField", "Oil"
                ), 2, 4));
        assertEquals("ART", art.get("type"));
        assertEquals("Artist", art.get("creator"));
        assertEquals("Oil", art.get("material"));

        Map<String, Object> vehicle = service.buildPayload(uploadTransport("V1"), user, "Vehicle", "Desc", "Xe co",
                List.of(image.toString()), fields(Map.of(
                        "#vehicleStartPriceField", "500000",
                        "#vehicleYearField", "2020",
                        "#vehicleOdometerField", "12000"
                ), 2, 4));
        assertEquals("VEHICLE", vehicle.get("type"));
        assertEquals("2020", vehicle.get("model"));
        assertEquals(12000, vehicle.get("odometer"));
    }

    @Test
    void buildPayloadBuildsOtherFieldsAndAllowsBlankReservePrice() throws Exception {
        Path image = imageFile("other.jpg");

        Map<String, Object> payload = service.buildPayload(uploadTransport("O1"), user, "Other", "Desc", "Khac",
                List.of(image.toUri().toString()), fields(Map.of(
                        "#otherStartPriceField", "100",
                        "#otherReservePriceField", "",
                        "#otherStepPriceField", "5"
                ), 2, 4));

        assertEquals("OTHER", payload.get("type"));
        assertEquals(0.0, (double) payload.get("reservePrice"), 0.001);
        assertEquals(5.0, (double) payload.get("minimumBidIncrement"), 0.001);
    }

    @Test
    void buildPayloadRejectsBadCategoryPriceTimeAndUploadResponses() throws Exception {
        Path image = imageFile("bad.jpg");

        assertThrows(AddAuctionProductClientService.ValidationException.class,
                () -> service.buildPayload(uploadTransport("IMG"), user, "Item", "Desc", "Unknown",
                        List.of(image.toString()), fields(Map.of(), 2, 4)));

        assertThrows(AddAuctionProductClientService.ValidationException.class,
                () -> service.buildPayload(uploadTransport("IMG"), user, "Item", "Desc", "Khac",
                        List.of(image.toString()), fields(Map.of(
                                "#otherStartPriceField", "abc",
                                "#otherReservePriceField", "",
                                "#otherStepPriceField", "5"
                        ), 2, 4)));

        assertThrows(AddAuctionProductClientService.ValidationException.class,
                () -> service.buildPayload(uploadTransport("IMG"), user, "Item", "Desc", "Khac",
                        List.of(image.toString()), fields(Map.of(
                                "#otherStartPriceField", "100",
                                "#otherReservePriceField", "50",
                                "#otherStepPriceField", "5"
                        ), 2, 4)));

        assertThrows(AddAuctionProductClientService.ValidationException.class,
                () -> service.buildPayload(uploadTransport("IMG"), user, "Item", "Desc", "Khac",
                        List.of(image.toString()), fields(Map.of(
                                "#otherStartPriceField", "100",
                                "#otherReservePriceField", "",
                                "#otherStepPriceField", "5"
                        ), -1, 1)));

        FakeMessageTransport failedUpload = new FakeMessageTransport()
                .respondWith(new Message(MessageType.UPLOAD_IMAGE, "ERROR", "upload rejected"));
        assertThrows(AddAuctionProductClientService.ValidationException.class,
                () -> service.buildPayload(failedUpload, user, "Item", "Desc", "Khac",
                        List.of(image.toString()), fields(Map.of(
                                "#otherStartPriceField", "100",
                                "#otherReservePriceField", "",
                                "#otherStepPriceField", "5"
                        ), 2, 4)));
    }

    @Test
    void buildPayloadContinuesWhenLaterImageFailsAfterOneSuccessfulUpload() throws Exception {
        Path image = imageFile("ok.jpg");
        FakeMessageTransport transport = uploadTransport("IMG1");

        Map<String, Object> payload = service.buildPayload(transport, user, "Item", "Desc", "Khac",
                List.of(image.toString(), tempDir.resolve("missing.jpg").toString()), fields(Map.of(
                        "#otherStartPriceField", "100",
                        "#otherReservePriceField", "",
                        "#otherStepPriceField", "5"
                ), 2, 4));

        assertEquals(List.of("IMG1"), payload.get("images"));
        assertEquals(1, transport.sentCount());
    }

    private Path imageFile(String name) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, new byte[]{1, 2, 3});
        return path;
    }

    private FakeMessageTransport uploadTransport(String imageId) {
        return new FakeMessageTransport().respondWith(new Message(MessageType.UPLOAD_IMAGE, (Object) imageId, "server"));
    }

    private AddAuctionProductClientService.CategoryFieldReader fields(Map<String, String> values,
                                                                      int startDaysFromNow,
                                                                      int endDaysFromNow) {
        return new AddAuctionProductClientService.CategoryFieldReader() {
            @Override
            public String fieldValue(String fieldId, String defaultValue) {
                return values.getOrDefault(fieldId, defaultValue);
            }

            @Override
            public LocalDate dateValue(String fieldId) {
                if (fieldId.toLowerCase().contains("start")) {
                    return LocalDate.now().plusDays(startDaysFromNow);
                }
                return LocalDate.now().plusDays(endDaysFromNow);
            }

            @Override
            public String comboValue(String fieldId) {
                return "00:00";
            }
        };
    }
}
