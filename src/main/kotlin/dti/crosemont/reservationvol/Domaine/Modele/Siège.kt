package dti.crosemont.reservationvol.Domaine.Modele

data class Siège(
    val id: Int,
    val numéroSiège: String,             
    val classe: String,
    var statut: String = "occupé"
)
