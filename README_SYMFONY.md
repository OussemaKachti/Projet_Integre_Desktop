# 🎓 UNICLUBS - Application Web de Gestion des Clubs (Symfony)

## 📑 Table des Matières
- [Description](#description)
- [Fonctionnalités Principales](#fonctionnalités-principales)
  - [Gestion des Utilisateurs](#gestion-des-utilisateurs)
  - [Gestion des Clubs](#gestion-des-clubs)
  - [Sondages et Intelligence Artificielle](#sondages-et-intelligence-artificielle)
  - [Événements](#événements)
  - [Compétitions](#compétitions)
  - [Gestion des Produits](#gestion-des-produits)
- [Technologies Utilisées](#technologies-utilisées)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Intégration avec l'Application Desktop](#intégration-avec-lapplication-desktop)
- [Contribution](#contribution)
- [Équipe de Développement](#équipe-de-développement)
- [Licence](#licence)
- [Remerciements](#remerciements)

## 📝 Description
UNICLUBS est une application web développée avec Symfony qui permet la gestion complète des clubs, événements, sondages et compétitions au sein de l'école ESPRIT. Cette application fait partie d'un système intégré qui comprend également une interface desktop développée avec JavaFX.

### 🎯 Objectif
Faciliter et centraliser la gestion des activités parascolaires au sein de l'école ESPRIT en offrant une plateforme web intuitive et complète.

### 🔍 Problème Résolu
- Fragmentation des outils de gestion des clubs
- Manque de centralisation des informations
- Difficulté de communication entre les clubs
- Complexité dans l'organisation des événements

## 🚀 Fonctionnalités Principales

### 👥 Gestion des Utilisateurs
- 🔐 Système d'authentification sécurisé
- 👤 Gestion des profils utilisateurs
- 🎭 Différents rôles (Administrateur, Président de Club, Membre, Non-Membre)
- ✉️ Système de vérification par email

### 🏢 Gestion des Clubs
- 📋 Création et gestion des clubs
- 👥 Gestion des membres
- 📊 Tableau de bord pour les présidents de clubs
- 📅 Planification des activités

### 📊 Sondages et Intelligence Artificielle
- 📝 Création et gestion des sondages
- 🤖 Fonctionnalités IA avancées :
  - 🛡️ Détection automatique des commentaires toxiques
  - 📊 Analyse des sentiments dans les commentaires
  - 📑 Génération automatique de résumés des commentaires
  - 🚫 Système de modération intelligent (ban automatique)
- 🎙️ Reconnaissance Vocale pour les Commentaires :
  - 🗣️ Saisie des commentaires par la voix
  - 🎤 Interface intuitive avec bouton d'enregistrement
  - ⚡ Transcription en temps réel
  - 🔄 Support multilingue pour la reconnaissance vocale
- 📈 Statistiques avancées :
  - 📊 Analyse des sentiments en temps réel
  - 📉 Tableaux de bord IA pour administrateurs
  - 📈 Visualisation des tendances utilisateur
- 💬 Système de commentaires intelligent
- 📈 Statistiques en temps réel

### 🎉 Événements
- 📅 Création et gestion d'événements
- 🎫 Système de participation
- 📍 Localisation des événements
- 📸 Gestion des médias

### 🏆 Compétitions
- 🎮 Organisation de compétitions
- 🏅 Système de classement
- 🎯 Suivi des scores
- 🏆 Gestion des récompenses

### 🛍️ Gestion des Produits
- 📦 Ajout et gestion des produits
- 💰 Gestion des prix et des stocks
- 🏷️ Catégorisation des produits
- 🛒 Système de commande
- 📊 Suivi des ventes

## 🛠️ Technologies Utilisées
- 🎨 Symfony 6.x
- 🐘 PHP 8.x
- 🗃️ MySQL
- 🎨 Twig Template Engine
- 🎭 JavaScript/jQuery
- 🎨 Bootstrap 5
- 🤖 Intégration IA :
  - 🧠 OpenAI API pour l'analyse de texte et la génération de résumés
  - 🔍 Hugging Face pour la détection de toxicité
  - 🎤 Web Speech API pour la reconnaissance vocale

## 📋 Prérequis
- 🐘 PHP 8.x ou supérieur
- 🗃️ MySQL Server
- 📦 Composer
- 🌐 Serveur Web (Apache/Nginx)
- 🔄 Node.js et npm

## ⚙️ Installation

1. **Cloner le repository**
```bash
git clone https://github.com/VotreOrganisation/uniclubs-symfony.git
cd uniclubs-symfony
```

2. **Installer les dépendances**
```bash
composer install
npm install
```

3. **Configurer le fichier .env**
```env
DATABASE_URL="mysql://user:password@127.0.0.1:3306/uniclubs_symfony"
# Configurer les autres variables d'environnement nécessaires
```

4. **Créer la base de données et effectuer les migrations**
```bash
php bin/console doctrine:database:create
php bin/console doctrine:migrations:migrate
```

5. **Compiler les assets**
```bash
npm run build
```

6. **Lancer le serveur de développement**
```bash
symfony server:start
```

## 📖 Utilisation

### Configuration Initiale
1. Accéder à l'application via votre navigateur
2. Se connecter avec les identifiants administrateur par défaut :
   - Email : admin@test.com
   - Mot de passe : Lina-lanouna1818

### Fonctionnalités Principales
- **Gestion des Clubs** : Créer, modifier et gérer les clubs
- **Événements** : Organiser et participer aux événements
- **Sondages et IA** : 
  - Créer et répondre aux sondages
  - Utiliser la reconnaissance vocale pour les commentaires
  - Modération automatique des commentaires
  - Analyse des sentiments
  - Génération de résumés
- **Compétitions** : Gérer les compétitions et suivre les scores
- **Produits** : Gérer le catalogue des produits, les stocks et les commandes

## 🔗 Intégration avec l'Application Desktop

Cette application web est intégrée avec une application desktop JavaFX, permettant une synchronisation en temps réel des données entre les deux plateformes. Les principales fonctionnalités d'intégration incluent :

- 🔄 Synchronisation des comptes utilisateurs
- 📊 Partage des données des événements
- 📝 Synchronisation des sondages et analyses IA
- 🏆 Mise à jour en temps réel des compétitions
- 🤖 Partage des analyses et statistiques IA

## 🤝 Contribution
Nous accueillons et apprécions toute contribution à UNICLUBS ! Voici comment vous pouvez contribuer :

1. **Fork** le projet
2. Créez votre branche de fonctionnalité (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

### Guide de Style
- Suivez les conventions de nommage PHP/Symfony
- Documentez les nouvelles fonctionnalités
- Ajoutez des tests unitaires pour les nouvelles fonctionnalités

## 👥 Équipe de Développement

- 👨‍💻 [Oussema KACHTI] - Team Lead & Full Stack Developer
  - Architecture globale du projet et intégration des modules
  - Développement du module Sondages (Frontend/Backend)
  - Intégration des technologies IA (OpenAI, Hugging Face, Web Speech API)
  - Gestion du versioning et Git
  - Conception des diagrammes UML
  - Tests et corrections des modules
  - Documentation technique

- 👩‍💻 Nour Balti - Full Stack Developer - Module Utilisateurs
  - Système d'authentification et autorisation
  - Gestion des profils utilisateurs
  - API des utilisateurs et intégration

- 👨‍💻 Mariem Trabelsi - Full Stack Developer - Module Clubs
  - Développement du système de gestion des clubs
  - Interface d'administration des clubs
  - API des clubs et intégration

- 👩‍💻 Imen Rzigui - Full Stack Developer - Module Événements
  - Système de gestion des événements
  - Interface de planification
  - API des événements et intégration

- 👨‍💻 Sirine Wahbi - Full Stack Developer - Module Produits
  - Système de gestion des produits
  - Interface e-commerce
  - API des produits et intégration

- 👩‍💻 Yassine Jomni - Full Stack Developer - Module Compétitions
  - Système de gestion des compétitions
  - Interface des tournois
  - API des compétitions et intégration

## 📄 Licence
Ce projet est sous licence MIT - voir le fichier [LICENSE.md](LICENSE.md) pour plus de détails.

## 🙏 Remerciements
- 🎓 ESPRIT pour le support et l'opportunité
- 👨‍🏫 Les encadrants pour leur guidance
- 🤝 Tous les contributeurs au projet
- 📚 La communauté open source pour les outils et bibliothèques utilisés 