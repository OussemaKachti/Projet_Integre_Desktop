-- Exemple d'insertion d'un club dans la table 'club'
-- Exécutez ces requêtes dans phpMyAdmin pour créer un club de test

-- Vérifier d'abord si la table existe et a la bonne structure
DESCRIBE club;

-- Insérer un club test (si nécessaire)
INSERT INTO club (nom, description, date_creation, status)
SELECT 'Club Test', 'Description du club test', NOW(), 'active'
WHERE NOT EXISTS (SELECT 1 FROM club LIMIT 1);

-- Vérifier que l'insertion a fonctionné
SELECT * FROM club;

-- Vérifier la relation avec la table sondage
DESCRIBE sondage;

-- Voir les sondages existants
SELECT * FROM sondage; 