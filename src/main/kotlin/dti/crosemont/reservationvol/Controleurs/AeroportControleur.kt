package dti.crosemont.reservationvol.Controleurs

import dti.crosemont.reservationvol.Controleurs.Exceptions.RequêteMalFormuléeException
import dti.crosemont.reservationvol.Domaine.Modele.Aeroport
import dti.crosemont.reservationvol.Domaine.Service.AeroportService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/aeroports")
class AeroportControleur(private val service: AeroportService) {

    @GetMapping
    fun obtenirTousLesAeroports(
            @RequestParam(name = "motCle", required = false) motCle: String?
    ): ResponseEntity<List<Aeroport>> {
        if (!motCle.isNullOrEmpty()) {
            return ResponseEntity.ok(service.obtenirAeroportsParNom(motCle))
        }
        return ResponseEntity.ok(service.obtenirTousLesAeroports())
    }

    @GetMapping("/{code}")
    fun obtenirUnAeroportParCode(@PathVariable code: String): ResponseEntity<Aeroport> {
        val aeroport = service.obtenirAeroportParCode(code)
        return if (aeroport != null) {
            ResponseEntity.ok(aeroport)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}")
    fun obtenirUnAeroportParId(@PathVariable id: Int): ResponseEntity<Aeroport> {
        val aeroport = service.obtenirAeroportParId(id)
        return if (aeroport != null) {
            ResponseEntity.ok(aeroport)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun ajouterAeroport(@RequestBody aeroport: Aeroport): ResponseEntity<Aeroport> {
        val aeroportAjoute = service.ajouterAeroport(aeroport)
        return ResponseEntity.ok(aeroportAjoute)
    }

    @PutMapping("/{id}")
    fun modifierAeroport(
            @PathVariable id: Int,
            @RequestBody aeroport: Aeroport
    ): ResponseEntity<Aeroport> {
        if (id == aeroport.id) {
            val aeroportModifie = service.modifierAeroport(aeroport)
            return if (aeroportModifie != null) {
                ResponseEntity.ok(aeroportModifie)
            } else {
                ResponseEntity.notFound().build()
            }
        } else {
            throw RequêteMalFormuléeException("Modification de l'aéroport invalide")
        }
    }

    @DeleteMapping("/{id}")
    fun supprimerAeroport(@PathVariable id: Int): ResponseEntity<HttpStatus> {
        service.supprimerUnAeroport(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
