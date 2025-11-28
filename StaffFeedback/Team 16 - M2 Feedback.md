# SwEnt M2 Team Grading Report

The M2 feedback provides an opportunity to give you, as a team, formal feedback on how you are performing in the project. By now, you should be building upon the foundations set in M1, achieving greater autonomy and collaboration within the team. This is meant to complement the informal, ungraded feedback from your coaches given during the weekly meetings or asynchronously on Discord, email, etc.

The feedback focuses on how well your team has applied good software engineering practices, delivered user value, and collaborated effectively. We assessed the quality and maintainability of your implementation, the clarity of your design, and the consistency of your delivery and teamwork across Sprints. An important component is how much you have progressed since the previous milestone. You can find the evaluation criteria in the [M2 Deliverables](https://github.com/swent-epfl/public/blob/main/project/M2.md) document. As mentioned in the past, the standards for M2 are elevated relative to M1, and this progression will continue into M3.

## Blue Belt

You qualified for a Blue Belt 🥋🔵 and got a final grade of 5.04/6 for M2. Excellent work! You're demonstrating advanced skills. 

We looked at several aspects, grouped into six categories. Here is the breakdown of the points you earned:

| Metric                                   | **Points Earned**              | **Weight** | **Feedback**                              |
|------------------------------------------|--------------------------------|------------|-------------------------------------------|
| **Implementation (APK, code quality)**   | 4.78 out of 6 | 40%        | [See Details](#implementation-apk-code-quality) |
| **Features**                             | 4.5 out of 6   | 20%        | [See Details](#features)                  |
| **Figma and Architecture Diagram**       | 6 out of 6 | 10%     | [See Details](#figma-and-architecture-diagram) |
| **Sprint Backlog & Product Backlog**     | 6 out of 6    | 10%        | [See Details](#sprint-backlog--product-backlog) |
| **Scrum Process (documents, autonomy)**  | 5.25 out of 6 | 10%       | [See Details](#scrum-process-documents-autonomy) |
| **Consistent Delivery of Value**         | 5 out of 6 | 10%      | [See Details](#consistent-delivery-of-value) |
| **Final Grade**                          | **5.04 out of 6**   |            |                                           |

In addition to the feedback you received from the coaches during the Sprints, you will find some extra feedback below.

---

## Implementation (APK, code quality)

We evaluated your APK’s functionality, stability, and user experience, along with the quality and consistency of your code. We also reviewed your CI setup, Sonar integration, and tests, including the presence of at least two meaningful end-to-end tests and the line coverage achieved.

Here are some bugs & issues we found in the APK:
• No back button when viewing the profile of an organization, preventing navigation back from that screen.
• Bottom navigation bar does not allow returning to the Events screen when viewing the map for a specific event.
• The back button icon design is inconsistent across the app.
• Users can create events in the past, which should be prevented.
• When editing an event, existing data is not pre-filled into the form fields.
• Event invitations provide no feedback, making it unclear whether an invitation was successfully sent.
• Users can invite themselves, which incorrectly assigns them the STAFF role rather than OWNER.
• The X button on event cards does not perform any action (no response or Toast).
• The design for the “Next Weekend” filter button could be improved.
• The “My Organizations” button incorrectly shows a + icon even though it only displays organizations rather than adding them.
• In My Events, the dropdown should open by default.
• Some text is hard to read due to dark text on dark background.
• There is no current location marker on the map.
• Organization creation form lacks validation for: phone number, name, description (should be required) and email (should be mandatory), 
• Organization ID is displayed instead of organization name on event cards.
• Inputs are not sanitized throughout the app.

For this part, you received 4.78 points out of a maximum of 6.

## Features

We evaluated the features implemented in this milestone. We looked for the completion of at least one epic, as well as the use of at least one public cloud service, one phone sensor, and a concept of authentication.
We assessed how well the implemented features align with the app’s objectives, integrate with existing functionality, and contribute to delivering clear user value.

The application includes several relevant features and demonstrates meaningful progress, but many of them feel unfinished and lack robustness. The implemented features are not yet polished, with noticeable usability issues and inconsistencies across different screens. Before expanding with new functionality, the team should focus on refining and stabilizing what is already built, improving navigation flows, validation, feedback to the user, and ensuring that existing features work reliably and cohesively end to end. Strengthening the current feature set will significantly improve the overall experience and value of the app.

For this part, you received 4.5 points out of a maximum of 6.

## Figma and Architecture Diagram

We evaluated whether your Figma and architecture diagram accurately reflect the current implementation of the app and how well they align with the app's functionality and structure. 

Both the figma and architecture diagrams are very good. Well done!

For this part, you received 6 points out of a maximum of 6.

## Sprint Backlog & Product Backlog

We assessed the structure and clarity of your Sprint and Product Backlogs and how you used them. We looked at whether your tasks are well defined, appropriately sized, and aligned with user stories; whether the Product Backlog is well organized and value-driven; and whether the Sprint Backlog is continuously updated and demonstrates good planning and prioritization.

Your scrum board looks good overall.

For this part, you received 6 points out of a maximum of 6.

## Scrum Process (documents, autonomy)

We evaluated your ability to autonomously run and document the Scrum process. We looked at how well you documented your team Stand-Ups and Retrospectives for each Sprint. We also assessed your level of autonomy in organizing and conducting these ceremonies, defining and prioritizing user stories in your Product Backlog, and planning well-scoped Sprint tasks that lead to concrete, valuable increments.

Scrum documentation and meetings were generally handled correctly and delivered on time, supporting smooth sprint transitions. Friday meetings were structured and roles were clear. The team shows good autonomy in managing Scrum without relying too much on coaches, though there is still space to push initiative further and depend even less on external guidance.

For this part, you received 5.25 points out of a maximum of 6.

## Consistent Delivery of Value

We reviewed your team’s ability to deliver meaningful increments of value at the end of each Sprint.  
We assessed whether your progress was steady, visible, and tied to concrete user value and app functionality, in line with your Product Backlog objectives.

The team delivered meaningful value consistently across the last sprints, with visible improvements and new usable functionality in each increment. Delivery appears well-organized and aligned with priorities. There is still some room for refinement and stronger polish to reach full consistency and robustness, but overall progress between sprints was clear and impactful.

For this part, you received 5 points out of a maximum of 6.

## Summary

Your team grade for milestone M2 is 5.04/6. If you are interested in how this fits into the bigger grading scheme, please see [project README](https://github.com/swent-epfl/public/blob/main/project/README.md) and the [course README](https://github.com/swent-epfl/public/blob/main/README.md).

Your coaches will be happy to discuss the above feedback in more detail.

Keep up the good work and good luck for the next milestone!