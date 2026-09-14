// Use an integer for version numbers
version = 1

cloudstream {
    // All of these properties are optional, you can safely remove any of them.
    description = "Nang Cuc"
    authors = listOf("lechanh", "ngoctan")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1 // Will be 3 if unspecified

    tvTypes = listOf("NSFW")
    //dong nay de kiem tra thu muc RES
    //requiresResources = false
    language = "vi"

    // Random CC logo I found
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"

// Class chính chứa @CloudstreamPlugin (ví dụ: com.NangCuc.NangCucPlugin)
    providerClass = "com.NangCuc.NangCucPlugin"
}
