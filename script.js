$(document).ready(function () {
  let heroes = [];

  // Handle CSV File Upload
  $("#file-upload").on("change", function (event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function (e) {
      const csvData = e.target.result;
      parseCSV(csvData);
    };
    reader.readAsText(file);
  });

  function parseCSV(csvData) {
    Papa.parse(csvData, {
      header: true,
      skipEmptyLines: true,
      complete: function (results) {
        heroes = results.data;
        populateHeroDropdowns(heroes);
      }
    });
  }

  function populateHeroDropdowns(heroes) {
    const hero1Dropdown = $("#hero1-dropdown");
    const hero2Dropdown = $("#hero2-dropdown");

    // Clear previous options (except the default placeholder)
    hero1Dropdown.find("option:not(:first)").remove();
    hero2Dropdown.find("option:not(:first)").remove();

    // Filter out invalid hero names and sort alphabetically
    const validHeroes = heroes
        .filter(hero => hero.hero_name && hero.hero_name.trim() !== "")
        .sort((a, b) => a.hero_name.localeCompare(b.hero_name));

    validHeroes.forEach(hero => {
        const option = `<option value="${hero.hero_name}">${hero.hero_name}</option>`;
        hero1Dropdown.append(option);
        hero2Dropdown.append(option);
    });
}


  // Update hero stats on dropdown change
  $("#hero1-dropdown").on("change", function () {
    const heroName = $(this).val();
    const selectedHero = heroes.find(h => h.hero_name === heroName);
    updateHeroStats(selectedHero, "#hero1-stats");
    showBattlePrediction();
  });

  $("#hero2-dropdown").on("change", function () {
    const heroName = $(this).val();
    const selectedHero = heroes.find(h => h.hero_name === heroName);
    updateHeroStats(selectedHero, "#hero2-stats");
    showBattlePrediction();
  });

  function updateHeroStats(hero, containerId) {
    if (!hero) {
        $(containerId).empty();
        return;
    }

    // Construct the local image path
    let heroImagePath = "ALL HEROES/" + hero.hero_name + ".png";

    $(containerId).html(`
        <h3>${hero.hero_name}</h3>
        <img src="${heroImagePath}" alt="${hero.hero_name}" class="hero-image">
        <div class="stat-container">
            <p><strong>HP:</strong> ${hero.hp}</p>
            <div class="hp-bar-container">
                <div class="hp-bar" style="width: ${hero.hp / 10}%;"></div>
            </div>
        </div>
        <p><strong>Physical Attack:</strong> ${hero.physical_atk}</p>
        <p><strong>Magic Defense:</strong> ${hero.magic_defense}</p>
        <p><strong>Win Rate:</strong> ${hero.win_rate}%</p>
    `);
}

  function showBattlePrediction() {
    let hero1Name = $("#hero1-stats h3").text();
    let hero2Name = $("#hero2-stats h3").text();

    if (!hero1Name || !hero2Name) {
      $("#battle-result").hide();
      return;
    }

    let hero1 = heroes.find(h => h.hero_name === hero1Name);
    let hero2 = heroes.find(h => h.hero_name === hero2Name);
    if (!hero1 || !hero2) return;

    // Calculate weighted scores and probabilities
    let hero1Score = (parseFloat(hero1.hp) * 0.4) +
                     (parseFloat(hero1.physical_atk) * 0.4) +
                     (parseFloat(hero1.win_rate) * 0.2);

    let hero2Score = (parseFloat(hero2.hp) * 0.4) +
                     (parseFloat(hero2.physical_atk) * 0.4) +
                     (parseFloat(hero2.win_rate) * 0.2);

    let totalScore = hero1Score + hero2Score;
    let hero1Probability = ((hero1Score / totalScore) * 100).toFixed(2);
    let hero2Probability = ((hero2Score / totalScore) * 100).toFixed(2);

    // Update UI with probabilities and animate the progress bar
    $("#hero1-probability").text(`${hero1.hero_name}: ${hero1Probability}%`);
    $("#hero2-probability").text(`${hero2.hero_name}: ${hero2Probability}%`);
    $("#probability-fill").animate({ width: `${hero1Probability}%` }, 500);

    $("#battle-result").fadeIn();

    // Change the background image based on the hero with the higher percentage.
    if (parseFloat(hero1Probability) > parseFloat(hero2Probability)) {
      $("body").css("background-image", "url('ALL HEROES/" + hero1.hero_name + ".png')");
    } else if (parseFloat(hero2Probability) > parseFloat(hero1Probability)) {
      $("body").css("background-image", "url('ALL HEROES/" + hero2.hero_name + ".png')");
    } else {
      // In case of a tie (50/50), show the default background image.
      $("body").css("background-image", "url('ALL HEROES/MLBB.png')");
    }
  }

  // Show Most Durable Hero Button Handler (with Modal)
  $("#show-durable-hero").on("click", function() {
    if (heroes.length === 0) {
      alert("Please upload the CSV file first.");
      return;
    }

    // Count heroes by role (assuming each hero has a "role" property)
    let roleCounts = {};
    heroes.forEach(hero => {
      let role = hero.role;
      if (role) {
        roleCounts[role] = (roleCounts[role] || 0) + 1;
      }
    });

    // Find the role with the highest number of heroes.
    let mostPopulatedRole = null;
    let maxCount = 0;
    for (let role in roleCounts) {
      if (roleCounts[role] > maxCount) {
        maxCount = roleCounts[role];
        mostPopulatedRole = role;
      }
    }

    if (!mostPopulatedRole) {
      alert("No role information available in the CSV data.");
      return;
    }

    // Filter heroes that belong to the most populated role.
    let heroesInRole = heroes.filter(hero => hero.role === mostPopulatedRole);

    // Determine the hero with the highest physical defense within that role.
    // (Assuming the CSV has a "physical_defense" property)
    let mostDurableHero = heroesInRole.reduce((prev, current) => {
      return (parseFloat(current.physical_defense) > parseFloat(prev.physical_defense)) ? current : prev;
    });

    // Display the result in the modal.
    $("#durable-hero-result-modal").html(`
      <h2>Most Durable Hero</h2>
      <p>Most populated role: <strong>${mostPopulatedRole}</strong> (${maxCount} heroes)</p>
      <p>Hero: <strong>${mostDurableHero.hero_name}</strong> with Physical Defense: <strong>${mostDurableHero.physical_defense}</strong></p>
    `);
    $("#durableHeroModal").fadeIn();

    // Change the background image to the most durable hero's image.
    $("body").css("background-image", "url('ALL HEROES/" + mostDurableHero.hero_name + ".png')");
  });

  // Close modal when clicking the close button
  $(".close-button").on("click", function(){
    $("#durableHeroModal").fadeOut();
  });

  // Close modal when clicking outside the modal content
  $(window).on("click", function(event) {
    if ($(event.target).is("#durableHeroModal")) {
      $("#durableHeroModal").fadeOut();
    }
  });
});
