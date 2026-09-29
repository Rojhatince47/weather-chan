package io.github.rojhatince47.weatherchan.feature.city.domain.permission

/**
 * Konum izninin **sorulup sorulmadığını** kalıcı tutar.
 *
 * Arayüz bilinçli olarak dar: yalnız "sistem diyaloğu bir kez gösterildi mi"
 * bilgisi. İzin cevabı burada **tutulmaz**, çünkü bayrak cevabı temsil edemez:
 * kullanıcı "Bir daha sorma" dedikten sonra ayarlardan izni geri açmış olabilir.
 * Cevaba bayrak üzerinden karar vermek, izni verilmiş kullanıcıyı reddedilmiş
 * gösterebilirdi. Bu yüzden gerçek doğrulama her seferinde sistemde yapılır;
 * bayrak yalnız diyaloğun **tekrarlanmamasını** garanti eder.
 */
interface CityPermissions {
    suspend fun wasAsked(): Boolean

    suspend fun markAsked()
}
